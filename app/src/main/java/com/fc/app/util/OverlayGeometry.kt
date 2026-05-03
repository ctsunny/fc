package com.fc.app.util

import com.fc.app.data.model.TextAlignOption

data class OverlayBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

fun overlayBounds(
    anchorX: Float,
    anchorY: Float,
    contentWidth: Float,
    contentHeight: Float,
    align: TextAlignOption,
    padding: Float = 0f,
): OverlayBounds {
    val left = when (align) {
        TextAlignOption.LEFT -> anchorX
        TextAlignOption.CENTER -> anchorX - (contentWidth / 2f)
        TextAlignOption.RIGHT -> anchorX - contentWidth
    }

    return OverlayBounds(
        left = left - padding,
        top = anchorY - padding,
        width = contentWidth + (padding * 2f),
        height = contentHeight + (padding * 2f),
    )
}

fun clampOverlayAnchorX(
    desiredX: Float,
    contentWidth: Float,
    canvasWidth: Float,
    align: TextAlignOption,
): Float {
    if (canvasWidth <= 0f) return desiredX
    if (contentWidth >= canvasWidth) {
        return when (align) {
            TextAlignOption.LEFT -> 0f
            TextAlignOption.CENTER -> canvasWidth / 2f
            TextAlignOption.RIGHT -> canvasWidth
        }
    }

    val min = when (align) {
        TextAlignOption.LEFT -> 0f
        TextAlignOption.CENTER -> contentWidth / 2f
        TextAlignOption.RIGHT -> contentWidth
    }
    val max = when (align) {
        TextAlignOption.LEFT -> canvasWidth - contentWidth
        TextAlignOption.CENTER -> canvasWidth - (contentWidth / 2f)
        TextAlignOption.RIGHT -> canvasWidth
    }
    return desiredX.coerceIn(min, max)
}

fun clampOverlayAnchorY(
    desiredY: Float,
    contentHeight: Float,
    canvasHeight: Float,
): Float {
    if (canvasHeight <= 0f) return desiredY
    if (contentHeight >= canvasHeight) return 0f
    return desiredY.coerceIn(0f, canvasHeight - contentHeight)
}
