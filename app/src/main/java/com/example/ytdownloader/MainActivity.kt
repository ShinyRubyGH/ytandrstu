package com.example.ytdownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.ytdownloader.theme.YTDownloaderTheme
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Iniciando motor...") }
    var isInitialized by remember { mutableStateOf(false) }
    
    // Preview States
    var isFetchingInfo by remember { mutableStateOf(false) }
    var videoTitle by remember { mutableStateOf<String?>(null) }
    var videoThumbnail by remember { mutableStateOf<String?>(null) }
    
    // Download States
    var isDownloading by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf(0f) }
    val workManager = WorkManager.getInstance(context)

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

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { status = "Iniciando núcleo (1/2)..." }
                YoutubeDL.getInstance().init(application)
                FFmpeg.getInstance().init(application)
                
                withContext(Dispatchers.Main) { status = "Buscando actualizaciones (2/2)..." }
                YoutubeDL.getInstance().updateYoutubeDL(application, YoutubeDL.UpdateChannel.STABLE)
                
                withContext(Dispatchers.Main) {
                    status = "Listo para buscar"
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
        
        val images = listOf(R.drawable.foto1, R.drawable.foto2, R.drawable.foto3, R.drawable.foto4, R.drawable.foto5)
        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(images) { imageRes ->
                Image(painter = painterResource(id = imageRes), contentDescription = "Foto", contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)))
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
                    onValueChange = { 
                        url = it
                        videoTitle = null // Reset preview on url change
                    },
                    label = { Text("Enlace del video") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (videoTitle == null) {
                    Button(
                        onClick = { 
                            isFetchingInfo = true
                            status = "Buscando información..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val info = YoutubeDL.getInstance().getInfo(url)
                                    withContext(Dispatchers.Main) {
                                        videoTitle = info.title
                                        videoThumbnail = info.thumbnail
                                        isFetchingInfo = false
                                        status = "Video encontrado"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        status = "Error buscando: ${e.message}"
                                        isFetchingInfo = false
                                    }
                                }
                            }
                        },
                        enabled = url.isNotBlank() && !isFetchingInfo && isInitialized,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isFetchingInfo) "Buscando..." else "Buscar Video", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            if (videoThumbnail != null) {
                                AsyncImage(
                                    model = videoThumbnail,
                                    contentDescription = "Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(text = videoTitle ?: "", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }

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
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = { selectedOption = option; expanded = false }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            isDownloading = true
                            progressValue = 0f
                            status = "Iniciando descarga en segundo plano..."
                            
                            val inputData = Data.Builder()
                                .putString("URL", url)
                                .putString("FORMAT", selectedOption.first)
                                .putString("TITLE", videoTitle)
                                .build()

                            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                .setInputData(inputData)
                                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                .build()

                            workManager.enqueue(workRequest)

                            val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
                            val observer = object : androidx.lifecycle.Observer<WorkInfo> {
                                override fun onChanged(value: WorkInfo) {
                                    when (value.state) {
                                        WorkInfo.State.RUNNING -> {
                                            val p = value.progress.getInt("PROGRESS", 0)
                                            progressValue = p / 100f
                                            status = "Descargando: $p%"
                                        }
                                        WorkInfo.State.SUCCEEDED -> {
                                            status = "¡Completado! Guardado en Descargas."
                                            isDownloading = false
                                            liveData.removeObserver(this)
                                        }
                                        WorkInfo.State.FAILED -> {
                                            status = "Error en la descarga."
                                            isDownloading = false
                                            liveData.removeObserver(this)
                                        }
                                        WorkInfo.State.CANCELLED -> {
                                            isDownloading = false
                                            liveData.removeObserver(this)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                            liveData.observeForever(observer)
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isDownloading) "Descargando..." else "Descargar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
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
