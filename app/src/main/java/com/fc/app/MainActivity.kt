package com.fc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fc.app.ui.screens.CameraScreen
import com.fc.app.ui.screens.CaptureScreen
import com.fc.app.ui.screens.EditorScreen
import com.fc.app.ui.screens.ExportScreen
import com.fc.app.ui.screens.PresetEditScreen
import com.fc.app.ui.screens.SettingsScreen
import com.fc.app.ui.theme.FcTheme
import com.fc.app.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FcTheme { FcApp() } }
    }
}

@Composable
fun FcApp() {
    val navController = rememberNavController()
    val vm: EditorViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "capture") {
        composable("capture") {
            val hasDraft = remember { vm.hasDraft() }
            val preferredRatio = remember { vm.loadPreferredCaptureRatio() }
            CaptureScreen(
                onVideoSelected = { uri, ratio ->
                    vm.setVideoUriWithRatio(uri, ratio)
                    navController.navigate("editor") { launchSingleTop = true }
                },
                onCameraClick = {
                    navController.navigate("camera") { launchSingleTop = true }
                },
                onSettingsClick = {
                    navController.navigate("settings") { launchSingleTop = true }
                },
                hasDraft = hasDraft,
                onResumeDraft = {
                    if (vm.restoreDraft()) {
                        navController.navigate("editor") { launchSingleTop = true }
                    }
                },
                initialAspectRatio = preferredRatio,
                onAspectRatioSelected = { vm.savePreferredCaptureRatio(it) },
            )
        }
        composable("editor") {
            state.videoUri?.let { uri ->
                EditorScreen(
                    videoUri = uri,
                    viewModel = vm,
                    onExportClick = { navController.navigate("export") },
                    onBackClick = {
                        vm.saveDraft()
                        navController.popBackStack()
                    }
                )
            } ?: EditorFallbackScreen(
                title = "请先选择视频",
                actionLabel = "返回首页",
                onAction = { navController.popBackStack("capture", inclusive = false) }
            )
        }
        composable("export") {
            if (state.videoUri == null) {
                EditorFallbackScreen(
                    title = "当前没有可导出的视频",
                    actionLabel = "返回首页",
                    onAction = {
                        vm.clearProject()
                        navController.popBackStack("capture", inclusive = false)
                    }
                )
            } else {
                ExportScreen(
                    viewModel = vm,
                    onBackToEdit = { navController.popBackStack() },
                    onStartNew = {
                        vm.clearDraft()
                        vm.clearProject()
                        navController.popBackStack("capture", inclusive = false)
                    }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditPreset = {
                    navController.navigate("presetEdit") { launchSingleTop = true }
                }
            )
        }
        composable("presetEdit") {
            PresetEditScreen(
                viewModel = vm,
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable("camera") {
            val preferredRatio = remember { vm.loadPreferredCaptureRatio() }
            CameraScreen(
                onVideoSaved = { uri ->
                    vm.setVideoUriWithRatio(uri, preferredRatio)
                    navController.navigate("editor") {
                        popUpTo("capture")
                        launchSingleTop = true
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun EditorFallbackScreen(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(actionLabel)
        }
    }
}

