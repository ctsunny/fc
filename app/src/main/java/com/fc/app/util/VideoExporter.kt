package com.fc.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.fc.app.data.model.OverlayTextField
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Exports a video with text overlays using Android's media3-transformer library.
 * Replaces the ffmpeg-kit dependency with a fully self-contained Google Maven solution.
 */
@OptIn(UnstableApi::class)
class VideoExporter(private val context: Context) {

    private companion object {
        const val DEFAULT_VIDEO_WIDTH = 1080
        const val DEFAULT_VIDEO_HEIGHT = 1920
    }

    /**
     * Applies [overlays] to [inputFile] and writes the result to [outputFile].
     * Must be called from a coroutine; switches to the main thread internally as required by
     * the Transformer API.
     */
    suspend fun export(
        inputFile: File,
        outputFile: File,
        overlays: List<OverlayTextField>,
    ) {
        val (videoWidth, videoHeight) = getVideoDimensions(inputFile)
        val overlayBitmap = buildOverlayBitmap(overlays, videoWidth, videoHeight)

        withContext(Dispatchers.Main) {
            // OverlaySettings: scale (2, 2) places the overlay across the full NDC frame
            // (-1,-1)..(1,1) so it covers the video frame edge-to-edge.
            val overlaySettings = OverlaySettings.Builder()
                .setScale(2f, 2f)
                .build()

            val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(
                overlayBitmap,
                overlaySettings,
            )
            val overlayEffect = OverlayEffect(ImmutableList.of(bitmapOverlay))
            val effects = Effects(
                /* audioProcessors= */ emptyList(),
                /* videoEffects= */ listOf<Effect>(overlayEffect),
            )

            val editedMediaItem = EditedMediaItem.Builder(
                MediaItem.fromUri(Uri.fromFile(inputFile))
            )
                .setEffects(effects)
                .build()

            val transformer = Transformer.Builder(context).build()

            suspendCancellableCoroutine { continuation ->
                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult,
                    ) {
                        overlayBitmap.recycle()
                        continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        overlayBitmap.recycle()
                        continuation.resumeWithException(exportException)
                    }
                })

                transformer.start(editedMediaItem, outputFile.absolutePath)
                continuation.invokeOnCancellation { transformer.cancel() }
            }
        }
    }

    private fun getVideoDimensions(file: File): Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: DEFAULT_VIDEO_WIDTH
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: DEFAULT_VIDEO_HEIGHT
            Pair(w, h)
        } finally {
            retriever.release()
        }
    }

    private fun buildOverlayBitmap(
        overlays: List<OverlayTextField>,
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        for (field in overlays) {
            if (!field.isVisible || field.text.isBlank()) continue

            val hexColor = field.colorHex.trimStart('#').padStart(6, '0')
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = field.fontSize
                typeface = if (field.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                color = Color.parseColor("#$hexColor")
                if (field.hasShadow) setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText(
                field.text,
                field.xFraction * width,
                field.yFraction * height,
                paint,
            )
        }
        return bitmap
    }
}
