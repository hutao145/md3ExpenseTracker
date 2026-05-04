package com.example.expensetracker.autotrack

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.autotrack.core.DraftPresenter
import com.autotrack.core.DraftSink
import com.autotrack.core.HostMapping
import com.autotrack.core.RecordType
import com.autotrack.core.TransactionCandidate
import com.autotrack.core.TransactionDraft
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
import kotlinx.coroutines.withContext

class AutoTrackAccessibilityService : com.autotrack.android.AutoTrackAccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy {
        ExpenseRepository(database.expenseDao(), database.assetDao(), database)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences("ExpenseAppPrefs", Context.MODE_PRIVATE)
    }

    override fun createDraftPresenter(): DraftPresenter {
        return DraftPresenter { draft, callback -> callback.onConfirmed(draft) }
    }

    override fun createDraftSink(): DraftSink {
        return ExpenseTrackerDraftSink()
    }

    override fun createHostMapping(): HostMapping {
        return ExpenseTrackerHostMapping()
    }

    override fun getScanDelayMillis(): Long {
        return 350L
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private class ExpenseTrackerHostMapping : HostMapping {
        override fun mapCategory(candidate: TransactionCandidate?): String {
            if (candidate == null) return "其他"
            return when (candidate.type) {
                RecordType.INCOME -> candidate.categoryHint.ifBlank { "其他收入" }
                RecordType.EXPENSE -> candidate.categoryHint.ifBlank { "其他" }
                RecordType.LIABILITY -> candidate.categoryHint.ifBlank { "负债" }
                RecordType.LOAN -> candidate.categoryHint.ifBlank { "借贷" }
                else -> candidate.categoryHint.ifBlank { "其他" }
            }
        }

        override fun mapAccount(candidate: TransactionCandidate?): String {
            if (candidate == null) return ""
            val paymentMethod = candidate.paymentMethod
            val source = candidate.sourcePackage
            return when {
                paymentMethod.contains("花呗") -> "花呗"
                paymentMethod.contains("余额宝") -> "余额宝"
                paymentMethod.contains("余额") -> "支付宝"
                paymentMethod.contains("零钱") -> "微信"
                paymentMethod.contains("银行卡") -> "银行卡"
                paymentMethod.contains("云闪付") || source.contains("unionpay") -> "云闪付"
                source.contains("tencent.mm") -> "微信"
                source.contains("alipay") -> "支付宝"
                source.contains("pinduoduo") -> "拼多多"
                source.contains("jingdong") -> "京东"
                source.contains("meituan") -> "美团"
                source.contains("aweme") -> "抖音"
                else -> paymentMethod.ifBlank { candidate.sourceAppName }
            }
        }
    }

    private inner class ExpenseTrackerDraftSink : DraftSink {
        override fun save(draft: TransactionDraft) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val candidate = draft.candidate ?: return@launch
                    if (!acceptRecentSignature(candidate.signature)) return@launch

                    val amountCent = candidate.amount.toAmountCent()
                    if (amountCent <= 0L) return@launch

                    val type = when (candidate.type) {
                        RecordType.INCOME -> 1
                        else -> 0
                    }
                    val assets = database.assetDao().getAllAssetsSnapshot()
                    val assetId = matchAssetId(assets, draft, candidate)
                    repository.addExpense(
                        amountCent = amountCent,
                        type = type,
                        category = draft.mappedCategory.ifBlank {
                            if (type == 1) "其他收入" else "其他"
                        },
                        note = buildNote(candidate),
                        assetId = assetId,
                        dateMillis = candidate.transactionTimeMillis
                    )
                    ExpenseAppWidget().updateAll(applicationContext)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AutoTrackAccessibilityService,
                            "已自动记账：${candidate.amount}元",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "保存自动记账失败", e)
                }
            }
        }

        private fun acceptRecentSignature(signature: String): Boolean {
            if (signature.isBlank()) return true
            val now = System.currentTimeMillis()
            val lastSignature = sharedPreferences.getString(KEY_LAST_SIGNATURE, "") ?: ""
            val lastTime = sharedPreferences.getLong(KEY_LAST_SIGNATURE_TIME, 0L)
            if (signature == lastSignature && now - lastTime < RECENT_DUPLICATE_WINDOW_MS) {
                return false
            }
            sharedPreferences.edit()
                .putString(KEY_LAST_SIGNATURE, signature)
                .putLong(KEY_LAST_SIGNATURE_TIME, now)
                .apply()
            return true
        }

        private fun matchAssetId(
            assets: List<AssetEntity>,
            draft: TransactionDraft,
            candidate: TransactionCandidate
        ): Long? {
            val keys = listOf(
                draft.mappedAccount,
                candidate.paymentMethod,
                candidate.sourceAppName
            )
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return assets.firstOrNull { asset ->
                keys.any { key ->
                    key.contains(asset.name, ignoreCase = true) ||
                        asset.name.contains(key, ignoreCase = true)
                }
            }?.id
        }

        private fun buildNote(candidate: TransactionCandidate): String {
            return candidate.note.ifBlank {
                listOf(candidate.sourceAppName, candidate.merchant)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" ")
            }
        }

        private fun Double.toAmountCent(): Long {
            return BigDecimal.valueOf(this)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }

    companion object {
        private const val TAG = "AutoTrackService"
        private const val KEY_LAST_SIGNATURE = "auto_track_last_signature"
        private const val KEY_LAST_SIGNATURE_TIME = "auto_track_last_signature_time"
        private const val RECENT_DUPLICATE_WINDOW_MS = 10 * 60 * 1000L
    }
}
