package com.fc.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fc.app.R
import com.fc.app.util.AspectRatioOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onVideoSelected: (Uri, AspectRatioOption) -> Unit,
    onCameraClick: () -> Unit = {},
    onAiEditingClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    hasDraft: Boolean = false,
    onResumeDraft: () -> Unit = {},
    initialAspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
    onAspectRatioSelected: (AspectRatioOption) -> Unit = {},
) {
    var selectedRatio by remember { mutableStateOf(initialAspectRatio) }
    val snackbarHostState = remember { SnackbarHostState() }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onVideoSelected(it, selectedRatio) } }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("开始创作", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                "选择或拍摄视频，套用预设模板后导出",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Aspect ratio selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "画幅选择",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AspectRatioOption.entries) { option ->
                        FilterChip(
                            selected = selectedRatio == option,
                            onClick = {
                                selectedRatio = option
                                onAspectRatioSelected(option)
                            },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Button(
                onClick = { galleryLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("从相册选择视频", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCameraClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("拍摄新视频（高清）", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onAiEditingClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("AI 智能剪辑（自动字幕 + 封面）", style = MaterialTheme.typography.titleMedium)
            }

            if (hasDraft) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onResumeDraft,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("恢复草稿", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                "手动编辑：选视频 → 套模板 → 拖拽调位 → 导出\nAI 剪辑：选预设 → 拍摄 → 自动处理 → 复核导出",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}


