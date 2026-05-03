package com.fc.app.util

enum class AspectRatioOption(
    val label: String,
    /** null means "use source ratio" */
    val fixedRatio: Float?,
) {
    ORIGINAL("原视频", null),
    PORTRAIT_9_16("9:16", 9f / 16f),
    CLASSIC_3_4("3:4", 3f / 4f),
    SQUARE_1_1("1:1", 1f),
    LANDSCAPE_16_9("16:9", 16f / 9f);

    fun resolve(sourceAspectRatio: Float): Float =
        fixedRatio ?: sourceAspectRatio
}

data class VideoFrameSize(
    val width: Int,
    val height: Int,
) {
    val aspectRatio: Float
        get() = if (width <= 0 || height <= 0) DEFAULT_VIDEO_ASPECT_RATIO else width.toFloat() / height.toFloat()
}

fun calculateOutputFrameSize(
    inputWidth: Int,
    inputHeight: Int,
    requestedAspectRatio: Float,
): VideoFrameSize {
    if (inputWidth <= 0 || inputHeight <= 0) {
        return VideoFrameSize(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
    }

    val inputAspectRatio = inputWidth.toFloat() / inputHeight.toFloat()
    val (outputWidth, outputHeight) = if (requestedAspectRatio >= inputAspectRatio) {
        (inputHeight * requestedAspectRatio) to inputHeight.toFloat()
    } else {
        inputWidth.toFloat() to (inputWidth / requestedAspectRatio)
    }

    return VideoFrameSize(
        width = outputWidth.toInt().coerceAtLeast(1),
        height = outputHeight.toInt().coerceAtLeast(1),
    )
}
