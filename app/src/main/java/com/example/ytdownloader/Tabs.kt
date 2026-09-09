package com.example.ytdownloader

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(application: android.app.Application, initialUrl: String = "") {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(painterResource(android.R.drawable.ic_menu_save), contentDescription = null) },
                    label = { Text("Descargar") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(android.R.drawable.ic_menu_gallery), contentDescription = null) },
                    label = { Text("Galería") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (selectedTab == 0) {
                DownloaderTab(application, initialUrl)
            } else {
                GalleryTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTab() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val allFiles = downloadDir.listFiles()?.toList() ?: emptyList()
            files = allFiles.filter { it.extension == "mp4" || it.extension == "mp3" }
                .sortedByDescending { it.lastModified() }
        }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Tus Descargas", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        }
        if (files.isEmpty()) {
            item {
                Text("No hay descargas recientes aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(files) { file ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, if (file.extension == "mp4") "video/mp4" else "audio/mpeg")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(file.name, fontWeight = FontWeight.Medium)
                    Text("${file.length() / (1024 * 1024)} MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
