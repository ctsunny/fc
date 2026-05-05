package com.fc.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
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
import com.fc.app.data.PresetTemplates
import com.fc.app.data.UserPreset
import com.fc.app.data.model.StyleTemplate
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
    val userPresets by viewModel.userPresetsFlow.collectAsState()
    val context = LocalContext.current
    var expandedFieldId by remember { mutableStateOf<String?>(null) }

    val watermarkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setWatermarkUri(uri) }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {

            AspectRatioSelectorRow(
                selected = uiState.aspectRatioOption,
                onSelected = viewModel::updateAspectRatioOption
            )

            // Preset selector: load built-in templates or user presets; save current as preset
            PresetSelectorRow(
                userPresets = userPresets,
                builtInTemplates = PresetTemplates.all,
                onApplyUserPreset = { viewModel.applyUserPreset(it) },
                onApplyTemplate = { viewModel.applyTemplate(it) },
                onSavePreset = { name -> viewModel.saveCurrentAsPreset(name) },
            )

            // Preview area – outer black container takes more weight;
            // inner box is constrained to the selected aspect ratio so the canvas
            // accurately shows what the exported video will look like.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
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

            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 200.dp)
            ) {
                // Controls that used to sit between the video and the field list
                // are now inside the scroll area so the video preview can use the
                // freed vertical space.
                item {
                    Text(
                        stringResource(R.string.editor_preview_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                item {
                    WatermarkRow(
                        watermarkUri = uiState.watermarkUri,
                        watermarkAlpha = uiState.watermarkAlpha,
                        watermarkScale = uiState.watermarkScale,
                        onPickWatermark = { watermarkLauncher.launch("image/png") },
                        onAlphaChange = { viewModel.setWatermarkAlpha(it) },
                        onScaleChange = { viewModel.setWatermarkScale(it) },
                        onClear = { viewModel.clearWatermark() },
                    )
                }
                item {
                    FadeDurationRow(
                        fadeSecs = uiState.fadeDurationSecs,
                        onFadeSecsChange = viewModel::updateFadeDurationSecs
                    )
                }
                item {
                    FruitFilterRow(
                        filter1Enabled = uiState.fruitFilter1Enabled,
                        filter2Enabled = uiState.fruitFilter2Enabled,
                        onFilter1Toggle = viewModel::toggleFruitFilter1,
                        onFilter2Toggle = viewModel::toggleFruitFilter2,
                    )
                }
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
                        onFontFamily = { viewModel.updateFieldFontFamily(field.id, it) },
                        onHasBackground = { viewModel.updateFieldHasBackground(field.id, it) },
                        onBackgroundAlpha = { viewModel.updateFieldBackgroundAlpha(field.id, it) },
                        onBackgroundColor = { viewModel.updateFieldBackgroundColor(field.id, it) },
                    )
                }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSelectorRow(
    userPresets: List<UserPreset>,
    builtInTemplates: List<StyleTemplate>,
    onApplyUserPreset: (UserPreset) -> Unit,
    onApplyTemplate: (StyleTemplate) -> Unit,
    onSavePreset: (String) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var saveDialogVisible by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("预设", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(32.dp))

        // Dropdown button
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = "选择预设套用",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                // Built-in generic templates section
                val genericTemplates = builtInTemplates.filter { it.category != com.fc.app.data.model.TemplateCategory.FRUIT }
                val fruitTemplates = builtInTemplates.filter { it.category == com.fc.app.data.model.TemplateCategory.FRUIT }

                DropdownMenuItem(
                    text = {
                        Text(
                            "── 内置模板 ──",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {},
                    enabled = false
                )
                genericTemplates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.name, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onApplyTemplate(template)
                            dropdownExpanded = false
                        }
                    )
                }
                // Fruit promotional templates section
                if (fruitTemplates.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Text(
                                "── 🍎 水果促销模板 ──",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                    fruitTemplates.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.name, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onApplyTemplate(template)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
                // User presets section
                if (userPresets.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Text(
                                "── 我的预设 ──",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                    userPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onApplyUserPreset(preset)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Save current as preset
        IconButton(
            onClick = {
                saveNameInput = ""
                saveDialogVisible = true
            }
        ) {
            Icon(Icons.Default.Save, contentDescription = "保存为预设", tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (saveDialogVisible) {
        AlertDialog(
            onDismissRequest = { saveDialogVisible = false },
            title = { Text("保存当前为预设") },
            text = {
                OutlinedTextField(
                    value = saveNameInput,
                    onValueChange = { saveNameInput = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveNameInput.isNotBlank()) {
                            onSavePreset(saveNameInput.trim())
                            saveDialogVisible = false
                        }
                    },
                    enabled = saveNameInput.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogVisible = false }) { Text("取消") }
            }
        )
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
            if (fadeSecs == 0) "文字消失：关闭" else "文字消失：${fadeSecs}秒内",
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
                onHasBackgroundChange = onHasBackground,
                onBackgroundAlphaChange = onBackgroundAlpha,
                onBackgroundColorChange = onBackgroundColor,
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
            FruitFilter.entries.forEach { filter ->
                val enabled = when (filter) {
                    FruitFilter.WARM_FRUIT -> filter1Enabled
                    FruitFilter.FRESH_FRUIT -> filter2Enabled
                }
                val onToggle = when (filter) {
                    FruitFilter.WARM_FRUIT -> onFilter1Toggle
                    FruitFilter.FRESH_FRUIT -> onFilter2Toggle
                }
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

@Composable
private fun WatermarkRow(
    watermarkUri: Uri?,
    watermarkAlpha: Float,
    watermarkScale: Float,
    onPickWatermark: () -> Unit,
    onAlphaChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("水印", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(36.dp))
            OutlinedButton(
                onClick = onPickWatermark,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (watermarkUri != null) "已导入（点击更换）" else "导入 PNG 水印",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            if (watermarkUri != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "移除水印", modifier = Modifier.size(18.dp))
                }
            }
        }
        if (watermarkUri != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("透明度 ${(watermarkAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                Slider(value = watermarkAlpha, onValueChange = onAlphaChange, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("大小 ${(watermarkScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                Slider(value = watermarkScale, onValueChange = onScaleChange, valueRange = 0.05f..1f, modifier = Modifier.weight(1f))
            }
        }
    }
}
