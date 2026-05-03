package com.fc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fc.app.ui.screens.CaptureScreen
import com.fc.app.ui.screens.EditorScreen
import com.fc.app.ui.screens.ExportScreen
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

    NavHost(navController = navController, startDestination = "capture") {
        composable("capture") {
            CaptureScreen { uri ->
                vm.setVideoUri(uri)
                navController.navigate("editor")
            }
        }
        composable("editor") {
            val state by vm.uiState.collectAsState()
            state.videoUri?.let { uri ->
                EditorScreen(
                    videoUri = uri,
                    viewModel = vm,
                    onExportClick = { navController.navigate("export") },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable("export") {
            ExportScreen(
                viewModel = vm,
                onBackToEdit = { navController.popBackStack() },
                onStartNew = {
                    vm.clearFields()
                    navController.popBackStack("capture", inclusive = false)
                }
            )
        }
    }
}
