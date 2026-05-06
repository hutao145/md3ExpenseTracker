package com.example.expensetracker.autotrack

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.autotrack.core.NodeSnapshot
import com.autotrack.core.PageSnapshot
import com.autotrack.core.RecordType
import com.autotrack.core.RecognitionEngine
import com.autotrack.core.TransactionCandidate
import com.autotrack.recognizer.DefaultRecognizers
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.AssetEntity
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.widget.ExpenseAppWidget
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recognitionEngine by lazy { RecognitionEngine(DefaultRecognizers.create()) }
    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy {
        ExpenseRepository(database.expenseDao(), database.assetDao(), database)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        appendLog(packageName, "AutoTrack", "[notification] 通知监听服务已连接")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val sourcePackage = sbn.packageName.orEmpty()
        if (!isLikelyPaymentPackage(sourcePackage)) return

        val text = collectNotificationText(sbn.notification)
        if (text.isBlank()) {
            appendLog(sourcePackage, "AutoTrack", "[notification] 支付类通知无可读文本")
            return
        }

        appendLog(sourcePackage, readableAppName(sourcePackage), text)
        val candidate = recognizeNotification(sourcePackage, text) ?: run {
            appendLog(sourcePackage, "AutoTrack", "[notification] 通知文本未命中记账规则")
            return
        }

        saveCandidate(candidate, text)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun recognizeNotification(sourcePackage: String, text: String): TransactionCandidate? {
        val snapshot = PageSnapshot(
            sourcePackage,
            readableAppName(sourcePackage),
            NodeSnapshot(text, "", "Notification", "", null)
        )
        val result = recognitionEngine.recognize(snapshot)
        if (result.isMatched) return result.candidate

        val amount = findAmount(text) ?: return null
        val type = resolveType(text) ?: return null
        return TransactionCandidate.builder()
            .sourcePackage(sourcePackage)
            .sourceAppName(readableAppName(sourcePackage))
            .type(type)
            .amount(amount)
            .currencySymbol("¥")
            .categoryHint(if (type == RecordType.INCOME) "其他收入" else readableAppName(sourcePackage))
            .merchant(extractMerchant(text))
            .paymentMethod(readableAppName(sourcePackage))
            .note(text)
            .confidence(0.55)
            .build()
    }

    private fun saveCandidate(candidate: TransactionCandidate, rawText: String) {
        serviceScope.launch {
            try {
                val signature = buildSignature(candidate, rawText)
                if (!acceptRecentSignature(signature)) return@launch

                val amountCent = BigDecimal.valueOf(candidate.amount)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .toLong()
                if (amountCent <= 0L) return@launch

                val type = if (candidate.type == RecordType.INCOME) 1 else 0
                val assets = database.assetDao().getAllAssetsSnapshot()
                repository.addExpense(
                    amountCent = amountCent,
                    type = type,
                    category = candidate.categoryHint.ifBlank {
                        if (type == 1) "其他收入" else "其他"
                    },
                    note = candidate.note.ifBlank { rawText },
                    assetId = matchAssetId(assets, candidate),
                    dateMillis = candidate.transactionTimeMillis
                )
                ExpenseAppWidget().updateAll(applicationContext)
                appendLog(
                    candidate.sourcePackage,
                    "AutoTrack",
                    "[notification] 已通过支付通知自动记账：${candidate.amount}元"
                )
            } catch (e: Exception) {
                Log.e(TAG, "保存支付通知记账失败", e)
                appendLog(candidate.sourcePackage, "AutoTrack", "[notification] 保存失败：${e.message.orEmpty()}")
            }
        }
    }

    private fun collectNotificationText(notification: Notification?): String {
        val extras = notification?.extras ?: return ""
        val parts = mutableListOf<String>()
        fun add(value: CharSequence?) {
            val text = value?.toString()?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
            if (text.isNotBlank() && parts.none { it == text }) parts += text
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }
        return parts.joinToString(" ")
    }

    private fun resolveType(text: String): RecordType? {
        val incomeKeywords = listOf("收款", "已收款", "收款到账", "到账", "收入", "转账收款")
        if (incomeKeywords.any { text.contains(it) }) return RecordType.INCOME

        val expenseKeywords = listOf("支付成功", "付款成功", "已支付", "扣款", "消费", "支出", "付款", "支付")
        if (expenseKeywords.any { text.contains(it) }) return RecordType.EXPENSE
        return null
    }

    private fun findAmount(text: String): Double? {
        val patterns = listOf(
            Regex("[¥￥]\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Regex("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*元")
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val value = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            if (value > 0.0 && value < 1_000_000.0) return value
        }
        return null
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            Regex("向(.{1,24}?)(?:付款|支付)"),
            Regex("在(.{1,24}?)(?:消费|支付)"),
            Regex("来自(.{1,24}?)(?:的)?(?:收款|转账)")
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
        }.orEmpty()
    }

    private fun matchAssetId(assets: List<AssetEntity>, candidate: TransactionCandidate): Long? {
        val keys = listOf(candidate.paymentMethod, candidate.sourceAppName, candidate.note)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return assets.firstOrNull { asset ->
            keys.any { key ->
                key.contains(asset.name, ignoreCase = true) ||
                    asset.name.contains(key, ignoreCase = true)
            }
        }?.id
    }

    private fun buildSignature(candidate: TransactionCandidate, rawText: String): String {
        return listOf(
            candidate.sourcePackage,
            candidate.type.name,
            BigDecimal.valueOf(candidate.amount).setScale(2, RoundingMode.HALF_UP).toPlainString(),
            rawText.hashCode().toString()
        ).joinToString("|")
    }

    private fun acceptRecentSignature(signature: String): Boolean {
        val now = System.currentTimeMillis()
        val lastSignature = sharedPreferences.getString(KEY_LAST_NOTIFICATION_SIGNATURE, "") ?: ""
        val lastTime = sharedPreferences.getLong(KEY_LAST_NOTIFICATION_SIGNATURE_TIME, 0L)
        if (signature == lastSignature && now - lastTime < RECENT_DUPLICATE_WINDOW_MS) {
            return false
        }
        sharedPreferences.edit()
            .putString(KEY_LAST_NOTIFICATION_SIGNATURE, signature)
            .putLong(KEY_LAST_NOTIFICATION_SIGNATURE_TIME, now)
            .apply()
        return true
    }

    private fun appendLog(packageName: String, appName: String, text: String) {
        if (!sharedPreferences.getBoolean(KEY_IN_APP_LOG_ENABLED, true)) return
        val timestamp = java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val line = "$timestamp package=$packageName, app=$appName, text=$text\n\n"
        val oldLog = sharedPreferences.getString(KEY_IN_APP_LOG_TEXT, "").orEmpty()
        val newLog = (line + oldLog).take(MAX_IN_APP_LOG_LENGTH)
        sharedPreferences.edit().putString(KEY_IN_APP_LOG_TEXT, newLog).apply()
    }

    private fun readableAppName(packageName: String): String {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("tencent.mm") -> "微信"
            pkg.contains("alipay") -> "支付宝"
            pkg.contains("unionpay") -> "云闪付"
            pkg.contains("pinduoduo") -> "拼多多"
            pkg.contains("jingdong") -> "京东"
            pkg.contains("meituan") -> "美团"
            pkg.contains("aweme") -> "抖音"
            else -> packageName
        }
    }

    private fun isLikelyPaymentPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("tencent.mm") ||
            pkg.contains("alipay") ||
            pkg.contains("unionpay") ||
            pkg.contains("pinduoduo") ||
            pkg.contains("jingdong") ||
            pkg.contains("meituan") ||
            pkg.contains("aweme")
    }

    companion object {
        private const val TAG = "PaymentNotificationListener"
        private const val PREFS_NAME = "ExpenseAppPrefs"
        private const val KEY_IN_APP_LOG_ENABLED = "autotrack_in_app_log_enabled"
        private const val KEY_IN_APP_LOG_TEXT = "autotrack_in_app_log_text"
        private const val KEY_LAST_NOTIFICATION_SIGNATURE = "auto_track_last_notification_signature"
        private const val KEY_LAST_NOTIFICATION_SIGNATURE_TIME = "auto_track_last_notification_signature_time"
        private const val RECENT_DUPLICATE_WINDOW_MS = 10 * 60 * 1000L
        private const val MAX_IN_APP_LOG_LENGTH = 30000
    }
}
