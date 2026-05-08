package com.fc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fc.app.viewmodel.AiEditingViewModel
import com.fc.app.viewmodel.AiPipelineStage

/**
 * AI 处理进度全屏页。
 * 展示五个阶段（音频提取 → 转录 → 优化 → 合成 → 完成）的实时进度。
 * 用户可随时取消。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProcessingScreen(
    viewModel: AiEditingViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val pipelineState by viewModel.pipelineState.collectAsState()

    // 当流水线完成后自动跳转
    LaunchedEffect(pipelineState.stage) {
        if (pipelineState.stage == AiPipelineStage.DONE) {
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 智能处理") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                pipelineState.stageLabel.ifBlank { "处理中…" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(24.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { pipelineState.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${(pipelineState.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )

            Spacer(Modifier.height(32.dp))

            // 阶段步骤列表
            StageRow("提取音频", pipelineState.stage, AiPipelineStage.EXTRACTING_AUDIO)
            StageRow("语音转文字", pipelineState.stage, AiPipelineStage.TRANSCRIBING)
            StageRow("AI 优化文案", pipelineState.stage, AiPipelineStage.REFINING)
            StageRow("合成视频", pipelineState.stage, AiPipelineStage.COMPOSITING)
            StageRow("完成", pipelineState.stage, AiPipelineStage.DONE)

            Spacer(Modifier.height(40.dp))

            if (pipelineState.stage == AiPipelineStage.ERROR) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pipelineState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("返回")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        viewModel.cancelPipeline()
                        onCancel()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("取消")
                }
            }
        }
    }
}

private val STAGE_ORDINALS = mapOf(
    AiPipelineStage.EXTRACTING_AUDIO to 0,
    AiPipelineStage.TRANSCRIBING to 1,
    AiPipelineStage.REFINING to 2,
    AiPipelineStage.COMPOSITING to 3,
    AiPipelineStage.DONE to 4,
)

@Composable
private fun StageRow(
    label: String,
    currentStage: AiPipelineStage,
    thisStage: AiPipelineStage,
) {
    val current = STAGE_ORDINALS[currentStage] ?: -1
    val target = STAGE_ORDINALS[thisStage] ?: 0
    val (icon, tint) = when {
        current > target -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        current == target -> Icons.Default.Pending to MaterialTheme.colorScheme.tertiary
        else -> Icons.Default.Pending to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
