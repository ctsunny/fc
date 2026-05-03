package com.fc.app.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.fc.app.data.model.OverlayTextField
import com.fc.app.data.model.TemplateCategory
import com.fc.app.R
import com.fc.app.ui.components.DraggableCanvas
import com.fc.app.ui.components.StylePanel
import com.fc.app.util.AspectRatioOption
import com.fc.app.util.DEFAULT_VIDEO_ASPECT_RATIO
import com.fc.app.util.readVideoDimensions
import com.fc.app.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    videoUri: Uri,
    viewModel: EditorViewModel,
    onExportClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedFieldId by remember { mutableStateOf<String?>(null) }
    val sourceAspectRatio by produceState(initialValue = DEFAULT_VIDEO_ASPECT_RATIO, key1 = videoUri) {
        value = readVideoDimensions(context, videoUri)?.aspectRatio?.takeIf { it > 0f } ?: DEFAULT_VIDEO_ASPECT_RATIO
    }
    val previewAspectRatio = uiState.aspectRatioOption.resolve(sourceAspectRatio)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑排版") },
                navigationIcon = { TextButton(onClick = onBackClick) { Text("返回") } },
                actions = {
                    Button(onClick = onExportClick, enabled = uiState.fields.isNotEmpty()) { Text("导出") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 模板选择（字段为空时展示）
            if (uiState.fields.isEmpty()) {
                TemplateSelectorRow { cat -> viewModel.applyPresetByCategory(cat) }
            }

            AspectRatioSelectorRow(
                selected = uiState.aspectRatioOption,
                onSelected = viewModel::updateAspectRatioOption
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspectRatio)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                DraggableCanvas(
                    fields = uiState.fields,
                    selectedFieldId = uiState.selectedFieldId,
                    onFieldSelected = { id ->
                        viewModel.selectField(id)
                        expandedFieldId = id
                    },
                    onFieldMoved = { id, x, y -> viewModel.updateFieldPosition(id, x, y) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (uiState.fields.isNotEmpty()) {
                Text(
                    stringResource(R.string.editor_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.fields, key = { it.id }) { field ->
                        FieldRow(
                            field = field,
                            isExpanded = expandedFieldId == field.id,
                            onToggle = {
                                val next = if (expandedFieldId == field.id) null else field.id
                                expandedFieldId = next
                                viewModel.selectField(next)
                            },
                            onVisibility = { viewModel.updateFieldVisibility(field.id, !field.isVisible) },
                            onText = { viewModel.updateFieldText(field.id, it) },
                            onSize = { viewModel.updateFieldFontSize(field.id, it) },
                            onColor = { viewModel.updateFieldColor(field.id, it) },
                            onBold = { viewModel.updateFieldBold(field.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioSelectorRow(
    selected: AspectRatioOption,
    onSelected: (AspectRatioOption) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("画面比例", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AspectRatioOption.entries) { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

@Composable
private fun TemplateSelectorRow(onSelect: (TemplateCategory) -> Unit) {
    val options = listOf(
        Triple(TemplateCategory.ECOMMERCE, "🛒 电商爆款", Color(0xFFFF4444)),
        Triple(TemplateCategory.STORE,     "🏪 实体门店", Color(0xFF2288FF)),
        Triple(TemplateCategory.ACTIVITY,  "🎉 活动促销", Color(0xFFFF6600))
    )
    Column(modifier = Modifier.padding(12.dp)) {
        Text("选择预设模板", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { (cat, name, color) ->
                Card(
                    onClick = { onSelect(cat) },
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f))
                ) {
                    Text(name, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: OverlayTextField,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onVisibility: () -> Unit,
    onText: (String) -> Unit,
    onSize: (Float) -> Unit,
    onColor: (String) -> Unit,
    onBold: (Boolean) -> Unit
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
                    Text(field.text, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1)
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
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}
