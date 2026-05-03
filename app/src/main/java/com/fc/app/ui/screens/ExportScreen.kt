package com.fc.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fc.app.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: EditorViewModel,
    onBackToEdit: () -> Unit,
    onStartNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!uiState.isExporting && uiState.exportedFileUri == null && uiState.exportMessage.isEmpty()) {
            viewModel.exportVideo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出视频") },
                navigationIcon = { TextButton(onClick = onBackToEdit) { Text("返回编辑") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.isExporting -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(uiState.exportMessage, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                uiState.exportedFileUri != null -> {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("导出成功！", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "视频已保存到 相册 / Movies / FCVideo 文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, uiState.exportedFileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享视频"))
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(10.dp))
                        Text("分享视频")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onStartNew, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("重新制作")
                    }
                }

                uiState.exportMessage.isNotEmpty() -> {
                    Text(uiState.exportMessage, style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBackToEdit) { Text("返回编辑") }
                }
            }
        }
    }
}
