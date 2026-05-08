package com.fc.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.fc.app.data.model.SubtitleSegment
import com.fc.app.viewmodel.AiEditingViewModel

/**
 * AI 剪辑结果复核页。
 *
 * - 视频缩略图预览
 * - 字幕列表（可逐条手动修改）
 * - 封面标题 / 副标题（可修改）
 * - 导出按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiEditorScreen(
    viewModel: AiEditingViewModel,
    videoUri: Uri,
    onExportDone: () -> Unit,
    onBack: () -> Unit,
) {
    val subtitles by viewModel.subtitleSegments.collectAsState()
    val coverText by viewModel.coverText.collectAsState()
    val savedUri by viewModel.savedUri.collectAsState()
    val context = LocalContext.current

    // 导出成功后自动跳转
    LaunchedEffect(savedUri) {
        if (savedUri != null) onExportDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("复核 AI 剪辑结果") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = {
                    Button(
                        onClick = { viewModel.exportFinal() },
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("保存到相册")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 视频缩略图
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoUri)
                            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                            .build(),
                        contentDescription = "视频缩略图",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }

            // 封面文案
            item {
                SectionHeader("封面文案")
            }
            item {
                OutlinedTextField(
                    value = coverText.title,
                    onValueChange = viewModel::updateCoverTitle,
                    label = { Text("封面标题（≤12字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = coverText.subtitle,
                    onValueChange = viewModel::updateCoverSubtitle,
                    label = { Text("封面副标题（≤20字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 字幕列表
            item {
                SectionHeader("字幕（${subtitles.size} 条）")
            }
            if (subtitles.isEmpty()) {
                item {
                    Text(
                        "未生成字幕（视频无人声或未配置 API Key）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                itemsIndexed(subtitles, key = { _, seg -> seg.id }) { idx, seg ->
                    SubtitleEditCard(
                        index = idx + 1,
                        segment = seg,
                        onTextChange = { viewModel.updateSubtitleText(seg.id, it) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SubtitleEditCard(
    index: Int,
    segment: SubtitleSegment,
    onTextChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "#$index  ${formatMs(segment.startMs)} → ${formatMs(segment.endMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = segment.text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 1,
                maxLines = 3,
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
