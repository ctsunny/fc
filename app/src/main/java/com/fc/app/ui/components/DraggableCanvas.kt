package com.fc.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.fc.app.data.model.OverlayTextField
import com.fc.app.data.model.TextAlignOption
import com.fc.app.util.clampOverlayAnchorX
import com.fc.app.util.clampOverlayAnchorY
import com.fc.app.util.overlayBounds
import com.fc.app.util.parseColorOrDefault

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
    var draggingAnchor by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val measuredFields = remember(fields, canvasSize, textMeasurer) {
        measureFields(
            fields = fields,
            textMeasurer = textMeasurer,
            canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        )
    }
    // rememberUpdatedState lets gesture lambdas always read the latest measuredFields
    // without recreating (and thus cancelling) the gesture handler on every field update.
    val measuredFieldsState = rememberUpdatedState(measuredFields)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                // Key = Unit: tap handler never needs to restart; latest fields are read
                // via measuredFieldsState.value at the time each tap fires.
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val hit = hitTest(measuredFieldsState.value, tap)
                        onFieldSelected(hit)
                    }
                }
                // Key = canvasSize only: restart when the canvas is resized (e.g. rotation)
                // but NOT on every field-position update, which previously cancelled the drag
                // on every frame and made dragging completely non-functional.
                .pointerInput(canvasSize) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fields = measuredFieldsState.value
                            val hitId = hitTest(fields, offset)
                            draggingFieldId = hitId
                            draggingAnchor = fields.firstOrNull { it.field.id == hitId }?.let {
                                Offset(it.anchorX, it.anchorY)
                            }
                            if (hitId != null) {
                                onFieldSelected(hitId)
                            }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val fid = draggingFieldId ?: return@detectDragGestures
                            val fields = measuredFieldsState.value
                            val measuredField = fields.firstOrNull { it.field.id == fid } ?: return@detectDragGestures
                            val currentAnchor = draggingAnchor ?: Offset(measuredField.anchorX, measuredField.anchorY)
                            val canvasWidth = canvasSize.width.toFloat()
                            val canvasHeight = canvasSize.height.toFloat()
                            if (canvasWidth <= 0f || canvasHeight <= 0f) return@detectDragGestures

                            val newAnchorX = clampOverlayAnchorX(
                                desiredX = currentAnchor.x + delta.x,
                                contentWidth = measuredField.layout.size.width.toFloat(),
                                canvasWidth = canvasWidth,
                                align = measuredField.field.textAlign
                            )
                            val newAnchorY = clampOverlayAnchorY(
                                desiredY = currentAnchor.y + delta.y,
                                contentHeight = measuredField.layout.size.height.toFloat(),
                                canvasHeight = canvasHeight
                            )

                            draggingAnchor = Offset(newAnchorX, newAnchorY)
                            val newX = newAnchorX / canvasWidth
                            val newY = newAnchorY / canvasHeight
                            onFieldMoved(fid, newX, newY)
                        },
                        onDragEnd = {
                            draggingFieldId = null
                            draggingAnchor = null
                        },
                        onDragCancel = {
                            draggingFieldId = null
                            draggingAnchor = null
                        }
                    )
                }
        ) {
            measuredFields.forEach { field ->
                drawOverlayText(
                    textMeasurer = textMeasurer,
                    measuredField = field,
                    isSelected = field.field.id == selectedFieldId,
                    isDragging = field.field.id == draggingFieldId
                )
            }
        }
    }
}

private const val SelectionPadding = 6f
private const val BackgroundPadding = 8f

private data class MeasuredOverlayField(
    val field: OverlayTextField,
    val layout: TextLayoutResult,
    val anchorX: Float,
    val anchorY: Float,
)

private fun measureFields(
    fields: List<OverlayTextField>,
    textMeasurer: TextMeasurer,
    canvasSize: Size,
): List<MeasuredOverlayField> {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return emptyList()

    return fields.filter { it.isVisible && it.text.isNotBlank() }.map { field ->
        val layout = textMeasurer.measure(
            text = field.text,
            style = textStyleFor(field)
        )
        val anchorX = clampOverlayAnchorX(
            desiredX = field.xFraction * canvasSize.width,
            contentWidth = layout.size.width.toFloat(),
            canvasWidth = canvasSize.width,
            align = field.textAlign
        )
        val anchorY = clampOverlayAnchorY(
            desiredY = field.yFraction * canvasSize.height,
            contentHeight = layout.size.height.toFloat(),
            canvasHeight = canvasSize.height
        )
        MeasuredOverlayField(
            field = field,
            layout = layout,
            anchorX = anchorX,
            anchorY = anchorY
        )
    }
}

private fun hitTest(fields: List<MeasuredOverlayField>, tap: Offset): String? =
    fields.lastOrNull { field ->
        val bounds = overlayBounds(
            anchorX = field.anchorX,
            anchorY = field.anchorY,
            contentWidth = field.layout.size.width.toFloat(),
            contentHeight = field.layout.size.height.toFloat(),
            align = field.field.textAlign,
            padding = SelectionPadding
        )
        tap.x in bounds.left..bounds.right && tap.y in bounds.top..bounds.bottom
    }?.field?.id

private fun textStyleFor(field: OverlayTextField) = TextStyle(
    fontSize = field.fontSize.sp,
    fontWeight = if (field.isBold) FontWeight.Bold else FontWeight.Normal,
    color = Color(parseColorOrDefault(field.colorHex, android.graphics.Color.WHITE)),
    textAlign = when (field.textAlign) {
        TextAlignOption.CENTER -> TextAlign.Center
        TextAlignOption.RIGHT -> TextAlign.End
        TextAlignOption.LEFT -> TextAlign.Start
    }
)

private fun DrawScope.drawOverlayText(
    textMeasurer: TextMeasurer,
    measuredField: MeasuredOverlayField,
    isSelected: Boolean,
    isDragging: Boolean
) {
    val field = measuredField.field
    val contentBounds = overlayBounds(
        anchorX = measuredField.anchorX,
        anchorY = measuredField.anchorY,
        contentWidth = measuredField.layout.size.width.toFloat(),
        contentHeight = measuredField.layout.size.height.toFloat(),
        align = field.textAlign
    )

    // Background
    if (field.hasBackground) {
        val bgColor = Color(parseColorOrDefault(field.backgroundColorHex, android.graphics.Color.argb(136, 0, 0, 0)))
        val bgBounds = overlayBounds(
            anchorX = measuredField.anchorX,
            anchorY = measuredField.anchorY,
            contentWidth = measuredField.layout.size.width.toFloat(),
            contentHeight = measuredField.layout.size.height.toFloat(),
            align = field.textAlign,
            padding = BackgroundPadding
        )
        drawRoundRect(
            color = bgColor,
            topLeft = Offset(bgBounds.left, bgBounds.top),
            size = Size(bgBounds.width, bgBounds.height),
            cornerRadius = CornerRadius(12f, 12f)
        )
    }

    // Shadow
    if (field.hasShadow) {
        drawText(
            textMeasurer = textMeasurer,
            text = field.text,
            topLeft = Offset(contentBounds.left + 2f, contentBounds.top + 2f),
            style = textStyleFor(field).copy(color = Color.Black)
        )
    }

    // Text
    drawText(
        textLayoutResult = measuredField.layout,
        topLeft = Offset(contentBounds.left, contentBounds.top)
    )

    // Selection border
    if (isSelected || isDragging) {
        val borderColor = if (isDragging) Color.Yellow else Color(0xFF00BFFF)
        val borderBounds = overlayBounds(
            anchorX = measuredField.anchorX,
            anchorY = measuredField.anchorY,
            contentWidth = measuredField.layout.size.width.toFloat(),
            contentHeight = measuredField.layout.size.height.toFloat(),
            align = field.textAlign,
            padding = SelectionPadding
        )
        drawRect(
            color = borderColor,
            topLeft = Offset(borderBounds.left, borderBounds.top),
            size = Size(borderBounds.width, borderBounds.height),
            style = Stroke(width = 2f)
        )
    }
}
