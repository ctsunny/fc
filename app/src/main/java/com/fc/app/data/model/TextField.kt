package com.fc.app.data.model

import kotlinx.serialization.Serializable

/**
 * 视频上的一个文字覆层字段
 * 坐标以画布尺寸的百分比表示，兼容任意分辨率
 */
@Serializable
data class OverlayTextField(
    val id: String,
    val label: String,
    val text: String = "",
    val isVisible: Boolean = true,
    // 位置（0.0 ~ 1.0 百分比）
    val xFraction: Float = 0.1f,
    val yFraction: Float = 0.1f,
    // 样式
    val fontSize: Float = 24f,
    val colorHex: String = "#FFFFFF",
    val isBold: Boolean = false,
    val hasShadow: Boolean = true,
    val hasBackground: Boolean = false,
    val backgroundColorHex: String = "#88000000",
    val textAlign: TextAlignOption = TextAlignOption.LEFT
)

@Serializable
enum class TextAlignOption { LEFT, CENTER, RIGHT }
