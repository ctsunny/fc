package com.fc.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import androidx.media3.effect.Presentation
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
import kotlin.math.ceil

/**
 * Exports a video with text overlays using Android's media3-transformer library.
 * Replaces the ffmpeg-kit dependency with a fully self-contained Google Maven solution.
 */
@OptIn(UnstableApi::class)
class VideoExporter(private val context: Context) {

    /**
     * Applies [overlays] to [inputFile] and writes the result to [outputFile].
     * Must be called from a coroutine; switches to the main thread internally as required by
     * the Transformer API.
     */
    suspend fun export(
        inputFile: File,
        outputFile: File,
        overlays: List<OverlayTextField>,
        aspectRatioOption: AspectRatioOption,
    ) {
        val (videoWidth, videoHeight) = getVideoDimensions(inputFile)
        val sourceAspectRatio = if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else DEFAULT_VIDEO_ASPECT_RATIO
        val outputAspectRatio = aspectRatioOption.resolve(sourceAspectRatio)
        val outputFrameSize = calculateOutputFrameSize(videoWidth, videoHeight, outputAspectRatio)
        val overlayBitmap = buildOverlayBitmap(overlays, outputFrameSize.width, outputFrameSize.height)

        withContext(Dispatchers.Main) {
            val overlaySettings = OverlaySettings.Builder()
                .setScale(2f, 2f)
                .build()

            val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(
                overlayBitmap,
                overlaySettings,
            )
            val overlayEffect = OverlayEffect(ImmutableList.of(bitmapOverlay))
            val videoEffects = mutableListOf<Effect>(
                Presentation.createForAspectRatio(outputAspectRatio, 0),
                overlayEffect,
            )
            val effects = Effects(
                /* audioProcessors= */ emptyList(),
                /* videoEffects= */ videoEffects,
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
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
            if (rotation % 180 == 0) Pair(w, h) else Pair(h, w)
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

            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                // Compose preview uses `fontSize.sp`, which becomes scaled pixels at render time.
                // `scaledDensity` matches sp text sizing, while plain `density` would match dp sizing.
                // Convert here so exported text matches the on-screen preview more closely.
                textSize = field.fontSize * context.resources.displayMetrics.scaledDensity
                typeface = if (field.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                color = parseColorOrDefault(field.colorHex, Color.WHITE)
                if (field.hasShadow) setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            // StaticLayout needs a concrete width; use the widest explicit line so multiline
            // preview/export placement stays consistent without stretching shorter lines.
            val lines = field.text.lines()
            val layoutWidth = lines
                .maxOfOrNull { line -> ceil(paint.measureText(line.ifEmpty { " " }).toDouble()).toInt() }
                ?.coerceAtLeast(1)
                ?: 1

            val layout = StaticLayout.Builder
                .obtain(field.text, 0, field.text.length, paint, layoutWidth)
                .setAlignment(
                    when (field.textAlign) {
                        com.fc.app.data.model.TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
                        com.fc.app.data.model.TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
                        com.fc.app.data.model.TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    }
                )
                .setIncludePad(false)
                .build()

            val anchorX = clampOverlayAnchorX(
                desiredX = field.xFraction * width,
                contentWidth = layout.width.toFloat(),
                canvasWidth = width.toFloat(),
                align = field.textAlign
            )
            val anchorY = clampOverlayAnchorY(
                desiredY = field.yFraction * height,
                contentHeight = layout.height.toFloat(),
                canvasHeight = height.toFloat()
            )
            val bounds = overlayBounds(
                anchorX = anchorX,
                anchorY = anchorY,
                contentWidth = layout.width.toFloat(),
                contentHeight = layout.height.toFloat(),
                align = field.textAlign
            )

            if (field.hasBackground) {
                val backgroundBounds = overlayBounds(
                    anchorX = anchorX,
                    anchorY = anchorY,
                    contentWidth = layout.width.toFloat(),
                    contentHeight = layout.height.toFloat(),
                    align = field.textAlign,
                    padding = 8f
                )
                val backgroundPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = parseColorOrDefault(field.backgroundColorHex, Color.argb(136, 0, 0, 0))
                }
                canvas.drawRoundRect(
                    backgroundBounds.left,
                    backgroundBounds.top,
                    backgroundBounds.right,
                    backgroundBounds.bottom,
                    16f,
                    16f,
                    backgroundPaint
                )
            }

            canvas.save()
            canvas.translate(bounds.left, bounds.top)
            layout.draw(canvas)
            canvas.restore()
        }
        return bitmap
    }
}
