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
import com.fc.app.data.model.FontFamilyOption
import com.fc.app.data.model.OverlayTextField
import com.fc.app.R
import com.fc.app.ui.components.DraggableCanvas
import com.fc.app.ui.components.StylePanel
import com.fc.app.util.AspectRatioOption
import com.fc.app.util.FruitFilter
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

            AspectRatioSelectorRow(
                selected = uiState.aspectRatioOption,
                onSelected = viewModel::updateAspectRatioOption
            )

            // Preview area – outer black container takes all available weight space;
            // inner box is constrained to the selected aspect ratio so the canvas
            // accurately shows what the exported video will look like.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val fixedRatio = uiState.aspectRatioOption.fixedRatio
                val innerModifier = if (fixedRatio != null) {
                    Modifier.aspectRatio(fixedRatio)
                } else {
                    Modifier.fillMaxSize()
                }
                Box(modifier = innerModifier) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoUri)
                            .crossfade(true)
                            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    DraggableCanvas(
                        fields = uiState.fields,
                        selectedFieldId = uiState.selectedFieldId,
                        onFieldSelected = { id ->
                            viewModel.selectField(id)
                            expandedFieldId = id
                        },
                        onFieldMoved = { id, x, y -> viewModel.updateFieldPosition(id, x, y) },
                        onCanvasSizeChanged = { size -> viewModel.updatePreviewCanvasSize(size.width, size.height) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text(
                stringResource(R.string.editor_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            FadeDurationRow(
                fadeSecs = uiState.fadeDurationSecs,
                onFadeSecsChange = viewModel::updateFadeDurationSecs
            )

            FruitFilterRow(
                filter1Enabled = uiState.fruitFilter1Enabled,
                filter2Enabled = uiState.fruitFilter2Enabled,
                onFilter1Toggle = viewModel::toggleFruitFilter1,
                onFilter2Toggle = viewModel::toggleFruitFilter2,
            )

            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 200.dp)
            ) {
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
                        onBold = { viewModel.updateFieldBold(field.id, it) },
                        onFontFamily = { viewModel.updateFieldFontFamily(field.id, it) }
                    )
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
private fun FadeDurationRow(
    fadeSecs: Int,
    onFadeSecsChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (fadeSecs == 0) "文字淡出：关闭" else "文字淡出：${fadeSecs}秒",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(110.dp)
        )
        Slider(
            value = fadeSecs.toFloat(),
            onValueChange = { onFadeSecsChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.weight(1f)
        )
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
    onBold: (Boolean) -> Unit,
    onFontFamily: (FontFamilyOption) -> Unit = {},
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
                onFontFamilyChange = onFontFamily,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun FruitFilterRow(
    filter1Enabled: Boolean,
    filter2Enabled: Boolean,
    onFilter1Toggle: () -> Unit,
    onFilter2Toggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            "水果专业滤镜",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FruitFilter.entries.forEachIndexed { index, filter ->
                val enabled = if (index == 0) filter1Enabled else filter2Enabled
                val onToggle = if (index == 0) onFilter1Toggle else onFilter2Toggle
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.size(width = 44.dp, height = 24.dp)
                    )
                    Column {
                        Text(filter.label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            filter.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
