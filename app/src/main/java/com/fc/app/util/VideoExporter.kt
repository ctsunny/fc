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
import android.os.Handler
import android.os.Looper
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
import com.fc.app.util.toTypeface
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
     *
     * @param fadeDurationSecs seconds over which the overlay fades out at the end of the video.
     *   Pass 0 to keep the overlay visible for the entire duration.
     */
    suspend fun export(
        inputFile: File,
        outputFile: File,
        overlays: List<OverlayTextField>,
        aspectRatioOption: AspectRatioOption,
        fadeDurationSecs: Int = 0,
        previewCanvasWidth: Int = 0,
        fruitFilter1Enabled: Boolean = false,
        fruitFilter2Enabled: Boolean = false,
    ) {
        val (videoWidth, videoHeight) = getVideoDimensions(inputFile)
        val videoDurationUs = getVideoDurationUs(inputFile)
        val sourceAspectRatio = if (videoWidth > 0 && videoHeight > 0) {
            videoWidth.toFloat() / videoHeight.toFloat()
        } else {
            DEFAULT_VIDEO_ASPECT_RATIO
        }
        val outputAspectRatio = aspectRatioOption.resolve(sourceAspectRatio)
        val outputFrameSize = calculateOutputFrameSize(videoWidth, videoHeight, outputAspectRatio)
        val overlayBitmap = buildOverlayBitmap(overlays, outputFrameSize.width, outputFrameSize.height, previewCanvasWidth)

        withContext(Dispatchers.Main) {
            val fadeDurationUs = fadeDurationSecs.toLong() * 1_000_000L
            val bitmapOverlay: BitmapOverlay = if (fadeDurationUs > 0L && videoDurationUs > 0L) {
                FadeOutOverlay(overlayBitmap, videoDurationUs, fadeDurationUs)
            } else {
                BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
            }
            val overlayEffect = OverlayEffect(ImmutableList.of(bitmapOverlay))
            val videoEffects = mutableListOf<Effect>()
            // Fruit colour-grading filters are applied first (to the raw video signal)
            if (fruitFilter1Enabled) videoEffects.addAll(FruitFilter.WARM_FRUIT.buildEffects())
            if (fruitFilter2Enabled) videoEffects.addAll(FruitFilter.FRESH_FRUIT.buildEffects())
            videoEffects.add(Presentation.createForAspectRatio(outputAspectRatio, 0))
            videoEffects.add(overlayEffect)
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
                        if (!overlayBitmap.isRecycled) overlayBitmap.recycle()
                        continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (!overlayBitmap.isRecycled) overlayBitmap.recycle()
                        continuation.resumeWithException(exportException)
                    }
                })

                transformer.start(editedMediaItem, outputFile.absolutePath)
                continuation.invokeOnCancellation {
                    // Recycle the bitmap regardless of which thread triggers cancellation.
                    if (!overlayBitmap.isRecycled) overlayBitmap.recycle()
                    // Transformer.cancel() must be called on the main thread.
                    Handler(Looper.getMainLooper()).post { transformer.cancel() }
                }
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
        } catch (e: Exception) {
            // Fallback to a safe default when the file cannot be probed
            // (e.g. unsupported codec, corrupt file, or platform restriction).
            Pair(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
        } finally {
            retriever.release()
        }
    }

    private fun getVideoDurationUs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationMs * 1_000L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private fun buildOverlayBitmap(
        overlays: List<OverlayTextField>,
        width: Int,
        height: Int,
        previewCanvasWidth: Int = 0,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Scale factor: convert sp-based font sizes to bitmap pixels.
        // If the preview canvas width is known (reported from DraggableCanvas),
        // use it so the exported text is proportionally the same size as seen
        // in the editor preview.  Fall back to screen width when not available.
        val referenceWidthPx = if (previewCanvasWidth > 0) {
            previewCanvasWidth.toFloat()
        } else {
            context.resources.displayMetrics.widthPixels.toFloat()
        }
        val fontScale = context.resources.displayMetrics.scaledDensity * (width.toFloat() / referenceWidthPx)

        for (field in overlays) {
            if (!field.isVisible || field.text.isBlank()) continue

            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                // field.fontSize stores the editor's logical text size in sp units.
                // fontScale converts sp to bitmap pixels, accounting for both screen
                // density and the ratio between the export bitmap and screen widths.
                textSize = field.fontSize * fontScale
                typeface = field.fontFamily.toTypeface(field.isBold)
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

/**
 * A [BitmapOverlay] that keeps the overlay fully visible and then linearly fades it out
 * over the last [fadeDurationUs] microseconds of the video.
 */
@OptIn(UnstableApi::class)
private class FadeOutOverlay(
    private val bitmap: Bitmap,
    private val videoDurationUs: Long,
    private val fadeDurationUs: Long,
) : BitmapOverlay() {

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        // Ensure the fade window never starts before the video begins.
        val effectiveFadeDurationUs = fadeDurationUs.coerceAtMost(videoDurationUs)
        val fadeStartUs = videoDurationUs - effectiveFadeDurationUs
        val alpha = when {
            effectiveFadeDurationUs <= 0L -> 1f
            presentationTimeUs >= videoDurationUs -> 0f
            presentationTimeUs <= fadeStartUs -> 1f
            else -> {
                val elapsed = (presentationTimeUs - fadeStartUs).toFloat()
                val window = effectiveFadeDurationUs.toFloat()
                (1f - elapsed / window).coerceIn(0f, 1f)
            }
        }
        return OverlaySettings.Builder()
            .setAlphaScale(alpha.coerceIn(0f, 1f))
            .build()
    }
}


