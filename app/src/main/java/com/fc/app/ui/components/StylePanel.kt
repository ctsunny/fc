package com.fc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fc.app.data.model.FontFamilyOption
import com.fc.app.data.model.OverlayTextField
import com.fc.app.util.toComposeFontFamily

/** 8 个预设颜色供快速选取 */
val presetColors = listOf(
    "#FFFFFF", "#000000", "#FF3333", "#FF6600",
    "#FFE100", "#00CC44", "#0088FF", "#CC00FF"
)

/** Extract the alpha component (0–255) from a "#AARRGGBB" or "#RRGGBB" hex string. */
private fun alphaFromHex(hex: String): Int {
    val c = hex.removePrefix("#")
    return if (c.length == 8) c.substring(0, 2).toIntOrNull(16) ?: 136 else 255
}

/** Return a new hex with the alpha replaced, keeping the existing RGB. */
private fun setAlphaInHex(hex: String, alpha: Int): String {
    val c = hex.removePrefix("#")
    val rgb = if (c.length == 8) c.substring(2) else if (c.length == 6) c else "000000"
    return "#%02X$rgb".format(alpha.coerceIn(0, 255))
}

@Composable
fun StylePanel(
    field: OverlayTextField,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onColorChange: (String) -> Unit,
    onBoldChange: (Boolean) -> Unit,
    onFontFamilyChange: (FontFamilyOption) -> Unit = {},
    onHasBackgroundChange: (Boolean) -> Unit = {},
    onBackgroundAlphaChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            "编辑：${field.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = field.text,
            onValueChange = onTextChange,
            label = { Text(field.label) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Spacer(Modifier.height(8.dp))

        // 字号
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字号 ${field.fontSize.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(56.dp))
            IconButton(
                onClick = { onFontSizeChange(field.fontSize - 2f) },
                enabled = field.fontSize > 10f,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.TextDecrease, contentDescription = "减小")
            }
            Slider(
                value = field.fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 10f..80f,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onFontSizeChange(field.fontSize + 2f) },
                enabled = field.fontSize < 80f,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.TextIncrease, contentDescription = "增大")
            }
        }
        Spacer(Modifier.height(6.dp))

        // 颜色
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("颜色", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
            presetColors.forEach { hex ->
                val selected = field.colorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(if (selected) 30.dp else 26.dp)
                        .clip(CircleShape)
                        .background(runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White))
                        .clickable { onColorChange(hex) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 字体背景开关 + 透明度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字幕背景", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(56.dp))
            Switch(
                checked = field.hasBackground,
                onCheckedChange = onHasBackgroundChange,
                modifier = Modifier.size(width = 44.dp, height = 24.dp)
            )
        }
        if (field.hasBackground) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val currentAlpha = alphaFromHex(field.backgroundColorHex)
                Text(
                    "透明度 ${(currentAlpha * 100 / 255)}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(76.dp)
                )
                Slider(
                    value = currentAlpha.toFloat(),
                    onValueChange = { onBackgroundAlphaChange(it.toInt()) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 字体 — each chip renders its label in the actual font family for visual preview
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("字体", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FontFamilyOption.entries.size) { index ->
                val option = FontFamilyOption.entries[index]
                FilterChip(
                    selected = field.fontFamily == option,
                    onClick = { onFontFamilyChange(option) },
                    label = {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = option.toComposeFontFamily()
                            )
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 加粗
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("加粗", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
            IconToggleButton(checked = field.isBold, onCheckedChange = onBoldChange) {
                Icon(
                    Icons.Default.FormatBold, contentDescription = "加粗",
                    tint = if (field.isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Expose helpers for other files that compute background alpha from hex. */
fun alphaFromBackgroundHex(hex: String): Int {
    val c = hex.removePrefix("#")
    return if (c.length == 8) c.substring(0, 2).toIntOrNull(16) ?: 136 else 255
}

fun setAlphaInBackgroundHex(hex: String, alpha: Int): String {
    val c = hex.removePrefix("#")
    val rgb = if (c.length == 8) c.substring(2) else if (c.length == 6) c else "000000"
    return "#%02X$rgb".format(alpha.coerceIn(0, 255))
}

