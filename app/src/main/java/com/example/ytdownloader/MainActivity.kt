package com.example.ytdownloader

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ytdownloader.theme.YTDownloaderTheme
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            YTDownloaderTheme { 
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
                    DownloaderScreen(application) 
                } 
            }
        }
    }
}

@Composable
fun DownloaderScreen(application: android.app.Application) {
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Inicializando...") }
    var isDownloading by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(application)
                FFmpeg.getInstance().init(application)
                withContext(Dispatchers.Main) {
                    status = "Listo para descargar"
                    isInitialized = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "Error crítico de inicio: ${e.message}"
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Downloader Universal (Offline)", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL del video") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                isDownloading = true
                status = "Descargando... por favor espera."
                
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val uniqueId = UUID.randomUUID().toString()
                        val outtmpl = File(downloadDir, "%(title)s_$uniqueId.%(ext)s").absolutePath
                        
                        val request = YoutubeDLRequest(url)
                        request.addOption("-o", outtmpl)
                        
                        if (url.contains("tiktok.com") || url.contains("instagram.com")) {
                            request.addOption("-f", "bestvideo[vcodec*=h264]+bestaudio/best[vcodec*=h264]/bestvideo[vcodec^=avc]+bestaudio/best[vcodec^=avc]/best")
                            request.addOption("--merge-output-format", "mp4")
                            // We don't force FFmpeg convert here because youtubedl-android uses its own embedded ffmpeg,
                            // but we can pass basic args
                        } else {
                            request.addOption("-f", "bestvideo[ext=mp4][vcodec^=avc][height<=1080]+bestaudio[ext=m4a]/bestvideo[ext=mp4][height<=1080]+bestaudio[ext=m4a]/best[height<=1080]")
                            request.addOption("--merge-output-format", "mp4")
                        }
                        
                        YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                            status = "Progreso: $progress% - Faltan $etaInSeconds seg."
                        }
                        
                        withContext(Dispatchers.Main) {
                            status = "¡Descarga completada! Revisa tu carpeta de Descargas."
                            isDownloading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            status = "Error: ${e.message}"
                            isDownloading = false
                        }
                    }
                }
            },
            enabled = url.isNotBlank() && !isDownloading && isInitialized,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Descargar")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(status)
    }
}
