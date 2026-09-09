package com.example.ytdownloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val channelId = "ytd_downloads"
    private val notificationId = 1001

    override suspend fun doWork(): Result {
        val url = inputData.getString("URL") ?: return Result.failure()
        val format = inputData.getString("FORMAT") ?: return Result.failure()
        val title = inputData.getString("TITLE") ?: "Video"

        createNotificationChannel()

        // Start Foreground immediately
        setForeground(createForegroundInfo(title, 0))

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val uniqueId = UUID.randomUUID().toString().substring(0, 5)
            val request = YoutubeDLRequest(url)

            if (format == "mp3") {
                val outtmpl = File(downloadDir, "%(title)s_$uniqueId.%(ext)s").absolutePath
                request.addOption("-o", outtmpl)
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            } else {
                val outtmpl = File(downloadDir, "%(title)s_$uniqueId.mp4").absolutePath
                request.addOption("-o", outtmpl)
                
                if (url.contains("tiktok.com") || url.contains("instagram.com")) {
                    request.addOption("-f", "bestvideo[vcodec*=h264]+bestaudio/best[vcodec*=h264]/bestvideo[vcodec^=avc]+bestaudio/best[vcodec^=avc]/best")
                } else {
                    if (format == "1440") {
                        request.addOption("-f", "bestvideo[ext=mp4][vcodec^=avc]+bestaudio[ext=m4a]/bestvideo[ext=mp4]+bestaudio[ext=m4a]/best")
                    } else {
                        request.addOption("-f", "bestvideo[ext=mp4][vcodec^=avc][height<=${format}]+bestaudio[ext=m4a]/bestvideo[ext=mp4][height<=${format}]+bestaudio[ext=m4a]/best[height<=${format}]/best")
                    }
                }
                request.addOption("--merge-output-format", "mp4")
            }

            var lastProgress = -1
            YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                val intProgress = progress.toInt()
                if (intProgress != lastProgress) {
                    lastProgress = intProgress
                    // Update Notification
                    notificationManager.notify(notificationId, getNotification(title, intProgress))
                    // Update WorkManager Progress
                    setProgressAsync(workDataOf("PROGRESS" to intProgress))
                }
            }
            
            // Show completion notification (not foreground)
            val completedNotif = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Descarga Completada")
                .setContentText(title)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
            notificationManager.notify(1002, completedNotif)
            
            return Result.success()
        } catch (e: Exception) {
            val errorNotif = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Error en la descarga")
                .setContentText(e.message)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .build()
            notificationManager.notify(1002, errorNotif)
            return Result.failure(workDataOf("ERROR" to e.message))
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = getNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun getNotification(title: String, progress: Int): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Descargando: $title")
            .setContentText("Progreso: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW // Low priority prevents sound/vibration on every update
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
