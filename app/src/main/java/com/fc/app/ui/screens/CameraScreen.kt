package com.fc.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "CameraScreen"

/**
 * 内置 CameraX 拍摄页面，录制时使用设备支持的最高画质，
 * 替代系统原生相机（后者默认只有 480×640）。
 *
 * 镜头控制：
 *  - 双指捏合/张开 → 缩放
 *  - 底部滑块 → 精细缩放
 *  - 点击预览画面 → 对焦
 *  - 闪光灯按钮 → 开/关手电筒
 *  - 翻转按钮 → 前/后摄像头切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onVideoSaved: (Uri) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasPermissions by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    // Linear zoom 0f..1f
    var zoomLevel by remember { mutableFloatStateOf(0f) }

    // Count up seconds while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1_000L)
                if (isRecording) recordingSeconds++
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermissions = grants[Manifest.permission.CAMERA] == true &&
                grants[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    if (!hasPermissions) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("拍摄视频") },
                    navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("需要相机和麦克风权限才能拍摄", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    }) { Text("重新授权") }
                }
            }
        }
        return
    }

    // Bind camera once permissions are granted or lens changes
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(lensFacing, hasPermissions) {
        if (!hasPermissions) return@LaunchedEffect
        val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try { cont.resume(future.get()) }
                catch (e: Exception) { cont.resumeWithException(e) }
            }, ContextCompat.getMainExecutor(context))
            cont.invokeOnCancellation { future.cancel(true) }
        }
        cameraProvider = provider

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val qualitySelector = QualitySelector.from(
            Quality.HIGHEST,
            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
        )
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        val vc = VideoCapture.withOutput(recorder)
        videoCapture = vc

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, vc)
            camera = boundCamera
            // Reset state when switching cameras
            zoomLevel = 0f
            torchEnabled = false
            boundCamera.cameraControl.setLinearZoom(0f)
            boundCamera.cameraControl.enableTorch(false)
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind error", e)
        }
    }

    // Apply zoom level changes to camera
    LaunchedEffect(zoomLevel) {
        camera?.cameraControl?.setLinearZoom(zoomLevel.coerceIn(0f, 1f))
    }

    // Apply torch state changes to camera
    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            cameraProvider?.unbindAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍摄视频") },
                navigationIcon = { TextButton(onClick = {
                    recording?.stop()
                    onCancel()
                }) { Text("取消") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Camera preview with pinch-to-zoom and tap-to-focus
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Pinch-to-zoom gesture
                        detectTransformGestures { _, _, zoom, _ ->
                            val currentZoom = zoomLevel
                            // Each pinch event multiplies zoom by the scale factor;
                            // map it to the linear 0..1 range incrementally.
                            val newZoom = (currentZoom + (zoom - 1f) * 0.5f).coerceIn(0f, 1f)
                            zoomLevel = newZoom
                        }
                    }
                    .pointerInput(Unit) {
                        // Tap-to-focus
                        detectTapGestures { tapOffset ->
                            val meteringPointFactory = previewView.meteringPointFactory
                            val point = meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                    }
            )

            // Controls overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Recording timer
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        val minutes = recordingSeconds / 60
                        val seconds = recordingSeconds % 60
                        Text(
                            "%02d:%02d".format(minutes, seconds),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Zoom slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "1×",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = zoomLevel,
                        onValueChange = { zoomLevel = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text(
                        "MAX",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Button row: Flash | Record/Stop | Switch camera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash / torch toggle (only for back camera)
                    IconButton(
                        onClick = { torchEnabled = !torchEnabled },
                        enabled = lensFacing == CameraSelector.LENS_FACING_BACK,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchEnabled) "关闭闪光灯" else "开启闪光灯",
                            tint = if (torchEnabled) Color.Yellow else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Record / Stop button
                    IconButton(
                        onClick = {
                            val vc = videoCapture ?: return@IconButton
                            if (isRecording) {
                                recording?.stop()
                            } else {
                                startRecording(context, vc) { savedUri ->
                                    isRecording = false
                                    recordingSeconds = 0
                                    if (savedUri != null) {
                                        onVideoSaved(savedUri)
                                    }
                                }.also { rec ->
                                    recording = rec
                                    isRecording = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                if (isRecording) Color.Red else Color.White,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "停止录制" else "开始录制",
                            tint = if (isRecording) Color.White else Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Switch camera
                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                torchEnabled = false
                                zoomLevel = 0f
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                    CameraSelector.LENS_FACING_FRONT
                                else
                                    CameraSelector.LENS_FACING_BACK
                            }
                        },
                        enabled = !isRecording,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "切换摄像头",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    onFinished: (Uri?) -> Unit,
): Recording {
    val outputFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
    val outputOptions = FileOutputOptions.Builder(outputFile).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        Log.e(TAG, "Recording error: ${event.cause}")
                        outputFile.delete()
                        onFinished(null)
                    } else {
                        onFinished(Uri.fromFile(outputFile))
                    }
                }
                else -> Unit
            }
        }
}
