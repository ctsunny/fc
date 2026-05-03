package com.fc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/** 8 个预设颜色供快速选取 */
val presetColors = listOf(
    "#FFFFFF", "#000000", "#FF3333", "#FF6600",
    "#FFE100", "#00CC44", "#0088FF", "#CC00FF"
)

@Composable
fun StylePanel(
    field: OverlayTextField,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onColorChange: (String) -> Unit,
    onBoldChange: (Boolean) -> Unit,
    onFontFamilyChange: (FontFamilyOption) -> Unit = {},
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

        // 字体
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("字体", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
            FontFamilyOption.entries.forEach { option ->
                FilterChip(
                    selected = field.fontFamily == option,
                    onClick = { onFontFamilyChange(option) },
                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
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
