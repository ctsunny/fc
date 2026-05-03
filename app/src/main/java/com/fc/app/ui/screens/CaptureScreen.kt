package com.fc.app.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.fc.app.R
import com.fc.app.util.AspectRatioOption
import com.fc.app.util.saveVideoFileToMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onVideoSelected: (Uri, AspectRatioOption) -> Unit,
    onSettingsClick: () -> Unit = {},
    hasDraft: Boolean = false,
    onResumeDraft: () -> Unit = {},
    initialAspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
    onAspectRatioSelected: (AspectRatioOption) -> Unit = {},
) {
    val context = LocalContext.current
    var tempVideoFile by remember { mutableStateOf<File?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedRatio by remember { mutableStateOf(initialAspectRatio) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Use rememberUpdatedState so launcher callbacks always read the latest selectedRatio
    // even if the user changes the chip selection while the camera/gallery is open.
    val currentSelectedRatio by rememberUpdatedState(selectedRatio)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onVideoSelected(it, currentSelectedRatio) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            val file = tempVideoFile
            val uri = tempVideoUri
            if (uri != null && file != null) {
                // Save original to gallery in background
                scope.launch(Dispatchers.IO) {
                    saveVideoFileToMediaStore(context, file)
                }
                onVideoSelected(uri, currentSelectedRatio)
            }
        } else {
            tempVideoFile?.delete()
            tempVideoFile = null
            tempVideoUri = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempVideoFile = file
            tempVideoUri = uri
            cameraLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("需要相机权限才能拍摄新视频")
            }
        }
    }

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
            // Company branding header
            Text(
                "湖北果旺角水果仓储",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

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
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("拍摄新视频", style = MaterialTheme.typography.titleMedium)
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
                "流程：选视频 → 套模板 → 拖拽调位 → 导出",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}
