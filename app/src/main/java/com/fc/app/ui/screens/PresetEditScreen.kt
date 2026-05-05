package com.fc.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fc.app.data.model.FontFamilyOption
import com.fc.app.data.model.OverlayTextField
import com.fc.app.ui.components.DraggableCanvas
import com.fc.app.ui.components.StylePanel
import com.fc.app.viewmodel.EditorViewModel

/**
 * 预设编辑页面——界面与 EditorScreen 完全一致，
 * 只是用深色画布替代视频预览，直接编辑已保存的预设内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditScreen(
    viewModel: EditorViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.presetEditState.collectAsState()
    var expandedFieldId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑预设：${state.presetName}") },
                navigationIcon = {
                    TextButton(onClick = {
                        viewModel.cancelPresetEdit()
                        onCancel()
                    }) { Text("取消") }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.savePresetEdit()
                            onSave()
                        },
                        enabled = state.fields.isNotEmpty()
                    ) { Text("保存") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {

            // 深色预览画布（与编辑界面保持一致，但无视频背景）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DraggableCanvas(
                        fields = state.fields,
                        selectedFieldId = state.selectedFieldId,
                        onFieldSelected = { id ->
                            viewModel.selectPresetEditField(id)
                            expandedFieldId = id
                        },
                        onFieldMoved = { id, x, y ->
                            viewModel.updatePresetEditFieldPosition(id, x, y)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (state.fields.isEmpty()) {
                    Text(
                        "暂无文字字段",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 字段编辑列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 200.dp)
            ) {
                items(state.fields, key = { it.id }) { field ->
                    PresetFieldRow(
                        field = field,
                        isExpanded = expandedFieldId == field.id,
                        onToggle = {
                            val next = if (expandedFieldId == field.id) null else field.id
                            expandedFieldId = next
                            viewModel.selectPresetEditField(next)
                        },
                        onVisibility = {
                            viewModel.updatePresetEditFieldVisibility(field.id, !field.isVisible)
                        },
                        onText = { viewModel.updatePresetEditFieldText(field.id, it) },
                        onSize = { viewModel.updatePresetEditFieldFontSize(field.id, it) },
                        onColor = { viewModel.updatePresetEditFieldColor(field.id, it) },
                        onBold = { viewModel.updatePresetEditFieldBold(field.id, it) },
                        onFontFamily = { viewModel.updatePresetEditFieldFontFamily(field.id, it) },
                        onHasBackground = { viewModel.updatePresetEditFieldHasBackground(field.id, it) },
                        onBackgroundAlpha = { viewModel.updatePresetEditFieldBackgroundAlpha(field.id, it) },
                        onBackgroundColor = { viewModel.updatePresetEditFieldBackgroundColor(field.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetFieldRow(
    field: OverlayTextField,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onVisibility: () -> Unit,
    onText: (String) -> Unit,
    onSize: (Float) -> Unit,
    onColor: (String) -> Unit,
    onBold: (Boolean) -> Unit,
    onFontFamily: (FontFamilyOption) -> Unit = {},
    onHasBackground: (Boolean) -> Unit = {},
    onBackgroundAlpha: (Int) -> Unit = {},
    onBackgroundColor: (String) -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVisibility, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (field.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (field.isVisible) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(field.label, style = MaterialTheme.typography.labelMedium)
                if (field.text.isNotBlank()) {
                    Text(
                        field.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                }
            }
            Icon(
                if (isExpanded) Icons.Default.Check else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            StylePanel(
                field = field,
                onTextChange = onText,
                onFontSizeChange = onSize,
                onColorChange = onColor,
                onBoldChange = onBold,
                onFontFamilyChange = onFontFamily,
                onHasBackgroundChange = onHasBackground,
                onBackgroundAlphaChange = onBackgroundAlpha,
                onBackgroundColorChange = onBackgroundColor,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}
