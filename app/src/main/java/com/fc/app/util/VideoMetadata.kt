package com.fc.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

data class VideoDimensions(
    val width: Int,
    val height: Int,
) {
    val aspectRatio: Float
        get() = if (height <= 0 || width <= 0) 16f / 9f else width.toFloat() / height.toFloat()
}

private const val TAG = "VideoMetadata"

fun readVideoDimensions(context: Context, uri: Uri): VideoDimensions? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?: return null
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?: return null
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0

        if (rotation % 180 == 0) VideoDimensions(width, height) else VideoDimensions(height, width)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read video metadata for preview: $uri", e)
        null
    } finally {
        retriever.release()
    }
}
