package com.example.expensetracker.backup

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.data.remote.WebDavClient
import com.example.expensetracker.ui.util.centToYuanString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ScheduledBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val CHANNEL_ID = "backup_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "ExpenseAppPrefs"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            val csvContent = generateCsvContent()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "backup_$timestamp.csv"

            // 1. Save to local auto_backup directory
            val backupDir = File(applicationContext.filesDir, "auto_backup")
            if (!backupDir.exists()) backupDir.mkdirs()
            val localFile = File(backupDir, fileName)
            localFile.writeText(csvContent)

            // 2. Upload to WebDAV
            var webDavSuccess = false
            val url = prefs.getString("webdav_url", "") ?: ""
            val username = prefs.getString("webdav_username", "") ?: ""
            val password = prefs.getString("webdav_password", "") ?: ""
            val path = prefs.getString("webdav_path", "") ?: ""

            if (url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && path.isNotBlank()) {
                val fullUrl = if (url.endsWith("/")) url + path else "$url/$path"
                val dirUrl = if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
                val targetUrl = dirUrl + fileName

                val tempFile = File(applicationContext.cacheDir, "auto_backup_temp.csv")
                tempFile.writeText(csvContent)
                webDavSuccess = WebDavClient().uploadFile(targetUrl, username, password, tempFile)
                tempFile.delete()
            }

            // 3. Update last backup time
            prefs.edit().putLong("last_auto_backup_time", System.currentTimeMillis()).apply()

            // 4. Send notification
            val message = if (webDavSuccess) "本地 + WebDAV 备份成功" else "本地备份成功（WebDAV 未配置或失败）"
            showNotification(message)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            showNotification("自动备份失败: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun generateCsvContent(): String {
        val db = ExpenseDatabase.getInstance(applicationContext)
        val expenses = db.expenseDao().getAllExpensesSnapshot()

        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("日期,类型,分类,金额(元),备注\n")

        val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        expenses.forEach { entity ->
            val date = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(entity.createdAtEpochMillis),
                ZoneId.systemDefault()
            ).format(csvFormatter)

            val amount = centToYuanString(entity.amountCent)
            val typeStr = if (entity.type == 1) "收入" else "支出"
            val note = entity.note.replace("\"", "\"\"")
            val safeNote = if (note.contains(",") || note.contains("\n")) "\"$note\"" else note

            sb.append("$date,$typeStr,${entity.category},$amount,$safeNote\n")
        }

        return sb.toString()
    }

    private fun showNotification(message: String) {
        val nm = NotificationManagerCompat.from(applicationContext)

        // Create channel (required for API 26+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "自动备份",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "定时自动备份通知" }
        nm.createNotificationChannel(channel)

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("记账备份")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
