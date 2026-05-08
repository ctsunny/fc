package com.fc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fc.app.data.model.AiPreset
import com.fc.app.data.model.AiProvider
import com.fc.app.data.model.AiWorkflow
import com.fc.app.data.model.CaptionStyle
import com.fc.app.data.model.CoverStyle
import com.fc.app.data.model.SubtitleLang
import com.fc.app.viewmodel.AiEditingViewModel

/**
 * AI 预设编辑页。
 * 配置工作流类型、AI 服务商 / 模型、字幕样式、封面样式等。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPresetEditScreen(
    viewModel: AiEditingViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val preset by viewModel.editingPreset.collectAsState()
    val current = preset ?: return

    var name by remember(current.id) { mutableStateOf(current.name) }
    var workflow by remember(current.id) { mutableStateOf(current.workflow) }
    var provider by remember(current.id) { mutableStateOf(current.apiProvider) }
    var modelId by remember(current.id) { mutableStateOf(current.modelId) }
    var tokenBudget by remember(current.id) { mutableFloatStateOf(current.maxTokenBudget.toFloat()) }
    var subtitleLang by remember(current.id) { mutableStateOf(current.subtitleLang) }
    var productName by remember(current.id) { mutableStateOf(current.productName) }
    var priceText by remember(current.id) { mutableStateOf(current.priceText) }
    // Caption style
    var captionFontSize by remember(current.id) { mutableStateOf(current.captionStyle.fontSize) }
    var captionColorHex by remember(current.id) { mutableStateOf(current.captionStyle.colorHex) }
    var captionYFraction by remember(current.id) { mutableStateOf(current.captionStyle.yFraction) }
    var captionHasBg by remember(current.id) { mutableStateOf(current.captionStyle.hasBackground) }
    // Cover style
    var coverFrameFraction by remember(current.id) { mutableStateOf(current.coverStyle.framePositionFraction) }
    var coverTitleSize by remember(current.id) { mutableStateOf(current.coverStyle.titleFontSize) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (current.name.isBlank()) "新建 AI 预设" else "编辑预设") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateEditingPreset(
                                current.copy(
                                    name = name.ifBlank { "未命名预设" },
                                    workflow = workflow,
                                    apiProvider = provider,
                                    modelId = modelId.ifBlank { provider.defaultModel },
                                    maxTokenBudget = tokenBudget.toInt(),
                                    subtitleLang = subtitleLang,
                                    productName = productName,
                                    priceText = priceText,
                                    captionStyle = CaptionStyle(
                                        fontSize = captionFontSize,
                                        colorHex = captionColorHex,
                                        yFraction = captionYFraction,
                                        hasBackground = captionHasBg,
                                    ),
                                    coverStyle = CoverStyle(
                                        framePositionFraction = coverFrameFraction,
                                        titleFontSize = coverTitleSize,
                                    ),
                                )
                            )
                            viewModel.saveEditingPreset()
                            onSave()
                        }
                    ) { Text("保存") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 预设名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 工作流
            SectionLabel("工作流类型")
            AiWorkflow.entries.forEach { wf ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = workflow == wf, onClick = { workflow = wf })
                    Text(wf.label, modifier = Modifier.padding(top = 12.dp))
                }
            }

            HorizontalDivider()

            // 商品信息（封面文案用）
            SectionLabel("商品信息（封面文案生成用）")
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("商品名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("价格文字（如 "9.9元/斤"）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // AI 服务商
            SectionLabel("AI 服务商")
            AiProvider.entries.forEach { p ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = provider == p, onClick = {
                        provider = p
                        if (modelId.isBlank()) modelId = p.defaultModel
                    })
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(p.label)
                        Text(
                            p.defaultBaseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = modelId,
                onValueChange = { modelId = it },
                label = { Text("模型 ID（留空使用服务商默认）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(provider.defaultModel) },
            )

            HorizontalDivider()

            // Token 预算
            SectionLabel("Token 预算上限（每次调用）：${tokenBudget.toInt()}")
            Slider(
                value = tokenBudget,
                onValueChange = { tokenBudget = it },
                valueRange = 500f..4000f,
                steps = 14,
                modifier = Modifier.fillMaxWidth(),
            )

            // 字幕语言
            SectionLabel("字幕语言")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SubtitleLang.entries.forEach { lang ->
                    FilterChip(
                        selected = subtitleLang == lang,
                        onClick = { subtitleLang = lang },
                        label = { Text(lang.label) },
                    )
                }
            }

            HorizontalDivider()

            // 字幕样式
            SectionLabel("字幕样式")
            Text("字体大小：${captionFontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = captionFontSize,
                onValueChange = { captionFontSize = it },
                valueRange = 12f..48f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("纵向位置：${(captionYFraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = captionYFraction,
                onValueChange = { captionYFraction = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = captionHasBg, onCheckedChange = { captionHasBg = it })
                Spacer(Modifier.width(8.dp))
                Text("字幕背景遮罩")
            }

            HorizontalDivider()

            // 封面样式
            SectionLabel("封面样式")
            Text("截帧位置：${(coverFrameFraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = coverFrameFraction,
                onValueChange = { coverFrameFraction = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("封面标题大小：${coverTitleSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = coverTitleSize,
                onValueChange = { coverTitleSize = it },
                valueRange = 20f..60f,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
