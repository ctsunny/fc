package com.fc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fc.app.data.model.AiPreset
import com.fc.app.viewmodel.AiEditingViewModel

/**
 * AI 剪辑预设选择页。
 * 展示已保存的 AI 预设列表，用户选择一条后进入拍摄或选择视频流程。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPresetPickerScreen(
    viewModel: AiEditingViewModel,
    onPresetSelected: (AiPreset) -> Unit,
    onNewPreset: () -> Unit,
    onEditPreset: (AiPreset) -> Unit,
    onBack: () -> Unit,
) {
    val presets by viewModel.aiPresetsFlow.collectAsState()
    var deleteCandidate by remember { mutableStateOf<AiPreset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 智能剪辑") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPreset,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建 AI 预设") },
            )
        }
    ) { padding ->
        if (presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "还没有 AI 预设\n点击右下角按钮新建一个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(presets, key = { it.id }) { preset ->
                    AiPresetCard(
                        preset = preset,
                        onSelect = { onPresetSelected(preset) },
                        onEdit = { onEditPreset(preset) },
                        onDelete = { deleteCandidate = preset },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("删除 AI 预设") },
            text = { Text("确定要删除「${candidate.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAiPreset(candidate.id)
                    deleteCandidate = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AiPresetCard(
    preset: AiPreset,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${preset.workflow.label}  ·  ${preset.apiProvider.label}  ·  ${preset.modelId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                if (preset.productName.isNotBlank()) {
                    Text(
                        "商品：${preset.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Button(onClick = onSelect) { Text("使用") }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
