package com.fc.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.fc.app.data.model.OverlayTextField
import com.fc.app.data.model.TextAlignOption

/**
 * 可拖拽文字覆层画布
 * 点击选中字段；按住拖动调整位置
 */
@Composable
fun DraggableCanvas(
    fields: List<OverlayTextField>,
    selectedFieldId: String?,
    onFieldSelected: (String?) -> Unit,
    onFieldMoved: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var draggingFieldId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fields) {
                    detectTapGestures { tap ->
                        val hit = hitTest(fields, tap, Size(size.width.toFloat(), size.height.toFloat()))
                        onFieldSelected(hit)
                    }
                }
                .pointerInput(fields) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            draggingFieldId = hitTest(fields, offset, Size(size.width.toFloat(), size.height.toFloat()))
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val fid = draggingFieldId ?: return@detectDragGestures
                            val field = fields.firstOrNull { it.id == fid } ?: return@detectDragGestures
                            val newX = (field.xFraction + delta.x / size.width).coerceIn(0f, 1f)
                            val newY = (field.yFraction + delta.y / size.height).coerceIn(0f, 1f)
                            onFieldMoved(fid, newX, newY)
                        },
                        onDragEnd = { draggingFieldId = null },
                        onDragCancel = { draggingFieldId = null }
                    )
                }
        ) {
            fields.filter { it.isVisible && it.text.isNotBlank() }.forEach { field ->
                drawOverlayText(
                    textMeasurer = textMeasurer,
                    field = field,
                    canvasSize = size,
                    isSelected = field.id == selectedFieldId,
                    isDragging = field.id == draggingFieldId
                )
            }
        }
    }
}

private fun hitTest(fields: List<OverlayTextField>, tap: Offset, canvasSize: Size): String? =
    fields.filter { it.isVisible && it.text.isNotBlank() }.lastOrNull { f ->
        val x = f.xFraction * canvasSize.width
        val y = f.yFraction * canvasSize.height
        val w = f.text.length * f.fontSize * 0.6f
        val h = f.fontSize * 1.5f
        Rect(x - 8f, y - 8f, x + w + 8f, y + h + 8f).contains(tap)
    }?.id

private fun DrawScope.drawOverlayText(
    textMeasurer: TextMeasurer,
    field: OverlayTextField,
    canvasSize: Size,
    isSelected: Boolean,
    isDragging: Boolean
) {
    val x = field.xFraction * canvasSize.width
    val y = field.yFraction * canvasSize.height

    val textColor = runCatching { Color(android.graphics.Color.parseColor(field.colorHex)) }
        .getOrDefault(Color.White)

    val style = TextStyle(
        fontSize = field.fontSize.sp,
        fontWeight = if (field.isBold) FontWeight.Bold else FontWeight.Normal,
        color = textColor,
        textAlign = when (field.textAlign) {
            TextAlignOption.CENTER -> TextAlign.Center
            TextAlignOption.RIGHT -> TextAlign.End
            TextAlignOption.LEFT -> TextAlign.Start
        }
    )
    val measured = textMeasurer.measure(field.text, style)

    // Background
    if (field.hasBackground) {
        val bgColor = runCatching { Color(android.graphics.Color.parseColor(field.backgroundColorHex)) }
            .getOrDefault(Color(0x88000000.toInt()))
        drawRect(
            color = bgColor,
            topLeft = Offset(x - 4f, y - 4f),
            size = Size(measured.size.width + 8f, measured.size.height + 8f)
        )
    }

    // Shadow
    if (field.hasShadow) {
        drawText(textMeasurer, field.text, Offset(x + 2f, y + 2f), style.copy(color = Color.Black))
    }

    // Text
    drawText(textMeasurer, field.text, Offset(x, y), style)

    // Selection border
    if (isSelected || isDragging) {
        val borderColor = if (isDragging) Color.Yellow else Color(0xFF00BFFF)
        drawRect(
            color = borderColor,
            topLeft = Offset(x - 6f, y - 6f),
            size = Size(measured.size.width + 12f, measured.size.height + 12f),
            style = Stroke(width = 2f)
        )
    }
}
