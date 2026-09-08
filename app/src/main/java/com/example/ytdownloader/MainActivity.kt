package com.example.ytdownloader

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(application: android.app.Application) {
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Iniciando motor...") }
    var isDownloading by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf(0f) }

    // Dropdown state
    val options = listOf(
        "1080" to "Video MP4 (1080p)",
        "1440" to "Video MP4 (1440p+)",
        "720" to "Video MP4 (720p)",
        "480" to "Video MP4 (480p)",
        "mp3" to "Solo Audio (MP3)"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options[0]) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { status = "Iniciando núcleo (1/2)..." }
                YoutubeDL.getInstance().init(application)
                FFmpeg.getInstance().init(application)
                
                withContext(Dispatchers.Main) { status = "Buscando actualizaciones (2/2)..." }
                YoutubeDL.getInstance().updateYoutubeDL(application, YoutubeDL.UpdateChannel.STABLE)
                
                withContext(Dispatchers.Main) {
                    status = "Listo para descargar"
                    isInitialized = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "Error: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloader",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Universal & Offline",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Gallery Carousel
        val images = listOf(
            R.drawable.foto1,
            R.drawable.foto2,
            R.drawable.foto3,
            R.drawable.foto4,
            R.drawable.foto5
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(images) { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Enlace del video") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedOption.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Formato / Calidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        isDownloading = true
                        progressValue = 0f
                        status = "Iniciando descarga..."
                        
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val uniqueId = UUID.randomUUID().toString().substring(0, 5)
                                
                                val request = YoutubeDLRequest(url)
                                
                                if (selectedOption.first == "mp3") {
                                    val outtmpl = File(downloadDir, "%(title)s_$uniqueId.%(ext)s").absolutePath
                                    request.addOption("-o", outtmpl)
                                    request.addOption("-x")
                                    request.addOption("--audio-format", "mp3")
                                    request.addOption("--audio-quality", "0")
                                } else {
                                    val outtmpl = File(downloadDir, "%(title)s_$uniqueId.mp4").absolutePath
                                    request.addOption("-o", outtmpl)
                                    
                                    val h = selectedOption.first
                                    
                                    if (url.contains("tiktok.com") || url.contains("instagram.com")) {
                                        request.addOption("-f", "bestvideo[vcodec*=h264]+bestaudio/best[vcodec*=h264]/bestvideo[vcodec^=avc]+bestaudio/best[vcodec^=avc]/best")
                                    } else {
                                        if (h == "1440") {
                                            request.addOption("-f", "bestvideo[ext=mp4][vcodec^=avc]+bestaudio[ext=m4a]/bestvideo[ext=mp4]+bestaudio[ext=m4a]/best")
                                        } else {
                                            request.addOption("-f", "bestvideo[ext=mp4][vcodec^=avc][height<=${h}]+bestaudio[ext=m4a]/bestvideo[ext=mp4][height<=${h}]+bestaudio[ext=m4a]/best[height<=${h}]/best")
                                        }
                                    }
                                    request.addOption("--merge-output-format", "mp4")
                                }
                                
                                YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, _ ->
                                    progressValue = progress / 100f
                                    status = "Progreso: ${progress.toInt()}% - Faltan ${etaInSeconds}s"
                                }
                                
                                withContext(Dispatchers.Main) {
                                    status = "¡Completado! Guardado en Descargas."
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isDownloading) "Descargando..." else "Descargar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = status,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                AnimatedVisibility(visible = isDownloading) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
        }
    }
}
