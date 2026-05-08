package com.fc.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fc.app.data.model.AiProvider
import com.fc.app.data.model.SubtitleLang
import com.fc.app.viewmodel.AiEditingViewModel
import com.fc.app.viewmodel.AiSettingsState

/**
 * AI 剪辑全局设置卡片，挂载在 SettingsScreen 底部。
 * 包含：服务商选择、API Key（密文输入）、Base URL、默认模型、Token 预算、字幕语言。
 */
@Composable
fun AiSettingsSection(aiViewModel: AiEditingViewModel) {
    val savedSettings by aiViewModel.settingsState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // 本地可编辑副本
    var provider by remember(savedSettings) { mutableStateOf(savedSettings.provider) }
    var apiKey by remember(savedSettings) { mutableStateOf(savedSettings.apiKey) }
    var customBaseUrl by remember(savedSettings) { mutableStateOf(savedSettings.customBaseUrl) }
    var defaultModel by remember(savedSettings) { mutableStateOf(savedSettings.defaultModel) }
    var tokenBudget by remember(savedSettings) { mutableFloatStateOf(savedSettings.maxTokenBudget.toFloat()) }
    var subtitleLang by remember(savedSettings) { mutableStateOf(savedSettings.subtitleLang) }
    var showApiKey by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // 标题行（可展开/折叠）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "AI 剪辑设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "折叠" else "展开",
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // API 服务商
                    Text("API 服务商", style = MaterialTheme.typography.labelLarge)
                    AiProvider.entries.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = provider == p, onClick = {
                                provider = p
                                if (provider != AiProvider.CUSTOM) {
                                    customBaseUrl = ""
                                }
                                if (defaultModel.isBlank()) defaultModel = p.defaultModel
                            })
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(p.label)
                                if (p != AiProvider.CUSTOM) {
                                    Text(
                                        p.defaultBaseUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }

                    // 自定义 Base URL（仅 CUSTOM 时显示）
                    if (provider == AiProvider.CUSTOM) {
                        OutlinedTextField(
                            value = customBaseUrl,
                            onValueChange = { customBaseUrl = it },
                            label = { Text("自定义 Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://your-proxy.example.com") },
                        )
                    }

                    HorizontalDivider()

                    // API Key
                    Text("API Key", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { showApiKey = !showApiKey }) {
                                Text(if (showApiKey) "隐藏" else "显示")
                            }
                        },
                    )

                    HorizontalDivider()

                    // 默认模型
                    OutlinedTextField(
                        value = defaultModel,
                        onValueChange = { defaultModel = it },
                        label = { Text("默认模型 ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(provider.defaultModel) },
                    )

                    // Token 预算
                    Text(
                        "Token 预算上限（每次调用）：${tokenBudget.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(
                        value = tokenBudget,
                        onValueChange = { tokenBudget = it },
                        valueRange = 500f..4000f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 字幕语言
                    Text("字幕语言", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SubtitleLang.entries.forEach { lang ->
                            FilterChip(
                                selected = subtitleLang == lang,
                                onClick = { subtitleLang = lang },
                                label = { Text(lang.label) },
                            )
                        }
                    }

                    HorizontalDivider()

                    // 保存按钮
                    Button(
                        onClick = {
                            aiViewModel.saveSettings(
                                AiSettingsState(
                                    provider = provider,
                                    customBaseUrl = customBaseUrl,
                                    defaultModel = defaultModel.ifBlank { provider.defaultModel },
                                    apiKey = apiKey,
                                    maxTokenBudget = tokenBudget.toInt(),
                                    subtitleLang = subtitleLang,
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存 AI 设置")
                    }
                }
            }
        }
    }
}
