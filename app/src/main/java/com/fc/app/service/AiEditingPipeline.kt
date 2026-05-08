package com.fc.app.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.fc.app.data.model.AiPreset
import com.fc.app.data.model.AiWorkflow
import com.fc.app.data.model.CaptionStyle
import com.fc.app.data.model.CoverStyle
import com.fc.app.data.model.CoverText
import com.fc.app.data.model.FontFamilyOption
import com.fc.app.data.model.OverlayTextField
import com.fc.app.data.model.SubtitleSegment
import com.fc.app.data.model.SubtitleLang
import com.fc.app.data.model.TextAlignOption
import com.fc.app.util.VideoExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/**
 * AI 编辑流水线，五个串行阶段：
 *
 *  1. 音频提取  (AudioExtractor)
 *  2. ASR 转录  (AiApiClient.transcribe)
 *  3. LLM 优化  (AiApiClient.refineSubtitlesAndCover) — 与封面生成合并为 1 次调用
 *  4. 字幕时间轴重对齐  (本地，零 token)
 *  5. 视频合成  (VideoExporter)
 *
 * 调用方通过 [onStageChange] 回调接收阶段进度更新。
 */
@OptIn(UnstableApi::class)
class AiEditingPipeline(private val context: Context) {

    companion object {
        private const val TAG = "AiEditingPipeline"
        private const val COVER_OVERLAY_DURATION_MS = 3_000L
    }

    data class PipelineResult(
        val subtitles: List<SubtitleSegment>,
        val coverText: CoverText,
        val outputFile: File,
    )

    /**
     * 执行完整流水线。
     *
     * @param videoUri        视频源
     * @param preset          AI 预设
     * @param apiKey          API Key（由 AiPreferences 在调用方解密后传入）
     * @param effectiveBaseUrl 实际 base URL（服务商默认 URL 或自定义 URL）
     * @param onStageChange   阶段变化回调 (stageName, progressFraction 0..1)
     */
    suspend fun run(
        videoUri: Uri,
        preset: AiPreset,
        apiKey: String,
        effectiveBaseUrl: String,
        maxTokenBudget: Int,
        onStageChange: (stage: String, progress: Float) -> Unit = { _, _ -> },
    ): PipelineResult {
        val tempDir = context.cacheDir.resolve("ai_pipeline").also { it.mkdirs() }

        // ── 阶段 1：提取音频 ──────────────────────────────────────────────────
        onStageChange("提取音频", 0.05f)
        val audioFile = tempDir.resolve("audio_${UUID.randomUUID()}.mp4")
        val extractedAudio = runCatching {
            AudioExtractor.extract(context, videoUri, audioFile)
        }.getOrElse { e ->
            Log.e(TAG, "Audio extraction error", e)
            null
        }

        // ── 阶段 2：ASR 转录 ──────────────────────────────────────────────────
        onStageChange("语音转文字", 0.25f)
        val rawSegments: List<SubtitleSegment>
        val asrFullText: String

        if (extractedAudio != null && apiKey.isNotBlank()) {
            val client = AiApiClient(effectiveBaseUrl, apiKey, preset.modelId)
            val langCode = if (preset.subtitleLang == SubtitleLang.ZH_EN) "zh" else "zh"
            rawSegments = runCatching { client.transcribe(extractedAudio, langCode) }
                .getOrElse { e ->
                    Log.e(TAG, "ASR failed, skipping subtitles: $e")
                    emptyList()
                }
            asrFullText = rawSegments.joinToString(" ") { it.text }
        } else {
            rawSegments = emptyList()
            asrFullText = ""
        }

        // ── 阶段 3：LLM 优化（字幕 + 封面，合并 1 次调用） ───────────────────
        onStageChange("AI 优化文案", 0.50f)
        val withCover = preset.workflow == AiWorkflow.COVER_AND_SUBTITLE ||
                preset.workflow == AiWorkflow.FULL_AUTO

        var refinedSegments = rawSegments
        var coverText = CoverText()

        if (asrFullText.isNotBlank() && apiKey.isNotBlank()) {
            val client = AiApiClient(effectiveBaseUrl, apiKey, preset.modelId)
            val llmResult = runCatching {
                client.refineSubtitlesAndCover(
                    asrText = asrFullText,
                    productName = preset.productName,
                    priceText = preset.priceText,
                    maxTokenBudget = maxTokenBudget,
                    withCover = withCover,
                    subtitleLang = if (preset.subtitleLang == SubtitleLang.ZH_EN) "zh_en" else "zh",
                )
            }.getOrElse { e ->
                Log.e(TAG, "LLM refinement failed, using raw ASR text: $e")
                null
            }

            if (llmResult != null) {
                refinedSegments = realignSegments(rawSegments, llmResult.refinedText)
                coverText = llmResult.coverText
            }
        } else if (withCover) {
            // 无 ASR 文本时，仍可用预设里的品名/价格生成封面
            coverText = CoverText(
                title = preset.productName.take(12),
                subtitle = preset.priceText.take(20),
            )
        }

        // ── 阶段 4：视频合成 ──────────────────────────────────────────────────
        onStageChange("合成视频", 0.70f)
        val inputFile = uriToFile(videoUri, tempDir)
        val outputFile = tempDir.resolve("ai_out_${UUID.randomUUID()}.mp4")

        val overlayFields = buildOverlayFields(refinedSegments, preset.captionStyle) +
                buildCoverOverlayFields(coverText, preset.coverStyle, withCover)

        val exporter = VideoExporter(context)
        try {
            exporter.export(
                inputFile = inputFile,
                outputFile = outputFile,
                overlays = overlayFields,
                aspectRatioOption = com.fc.app.util.AspectRatioOption.PORTRAIT_9_16,
                fadeDurationSecs = 0,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            throw e
        } finally {
            extractedAudio?.delete()
            if (inputFile != uriToFileCached(videoUri)) inputFile.delete()
        }

        onStageChange("完成", 1.0f)
        return PipelineResult(
            subtitles = refinedSegments,
            coverText = coverText,
            outputFile = outputFile,
        )
    }

    // ─── 时间戳重对齐（本地，零 token） ──────────────────────────────────────

    /**
     * 将 LLM 优化后的纯文本按**字符比例**重新映射回原始时间戳。
     *
     * 算法：
     *  1. 以原始字符总量为基准，计算每个原始 segment 的字符占比范围
     *  2. 对优化后的文本按相同占比切割
     *  3. 保留原始时间戳，替换文字内容
     */
    private fun realignSegments(
        original: List<SubtitleSegment>,
        refinedText: String,
    ): List<SubtitleSegment> {
        if (original.isEmpty()) return emptyList()
        val originalFull = original.joinToString("") { it.text }
        if (originalFull.isEmpty()) return original

        val totalOrigLen = originalFull.length.toFloat()
        val trimmedRefined = refinedText.trim()
        var charCursor = 0

        return original.mapIndexed { idx, seg ->
            val startRatio = original.take(idx).sumOf { it.text.length }.toFloat() / totalOrigLen
            val endRatio = (original.take(idx + 1).sumOf { it.text.length }).toFloat() / totalOrigLen
            val from = (startRatio * trimmedRefined.length).roundToInt().coerceIn(0, trimmedRefined.length)
            val to = (endRatio * trimmedRefined.length).roundToInt().coerceIn(from, trimmedRefined.length)
            val slice = if (from < to) trimmedRefined.substring(from, to) else seg.text
            charCursor = to
            seg.copy(text = slice)
        }
    }

    // ─── OverlayTextField 构建 ────────────────────────────────────────────────

    private fun buildOverlayFields(
        segments: List<SubtitleSegment>,
        style: CaptionStyle,
    ): List<OverlayTextField> = segments.mapIndexed { idx, seg ->
        OverlayTextField(
            id = "caption_${seg.id}",
            label = "字幕${idx + 1}",
            text = seg.text,
            isVisible = seg.text.isNotBlank(),
            xFraction = 0.05f,
            yFraction = style.yFraction,
            fontSize = style.fontSize,
            colorHex = style.colorHex,
            isBold = style.isBold,
            hasShadow = style.hasShadow,
            hasBackground = style.hasBackground,
            backgroundColorHex = style.backgroundColorHex,
            textAlign = TextAlignOption.CENTER,
            fontFamily = style.fontFamily,
        )
    }

    private fun buildCoverOverlayFields(
        coverText: CoverText,
        style: CoverStyle,
        include: Boolean,
    ): List<OverlayTextField> {
        if (!include || (coverText.title.isBlank() && coverText.subtitle.isBlank())) return emptyList()
        val fields = mutableListOf<OverlayTextField>()
        if (coverText.title.isNotBlank()) {
            fields += OverlayTextField(
                id = "cover_title",
                label = "封面标题",
                text = coverText.title,
                isVisible = true,
                xFraction = 0.5f,
                yFraction = 0.35f,
                fontSize = style.titleFontSize,
                colorHex = style.titleColorHex,
                isBold = true,
                hasShadow = true,
                hasBackground = true,
                backgroundColorHex = style.titleBgColorHex,
                textAlign = TextAlignOption.CENTER,
                fontFamily = FontFamilyOption.BLACK,
            )
        }
        if (coverText.subtitle.isNotBlank()) {
            fields += OverlayTextField(
                id = "cover_subtitle",
                label = "封面副标题",
                text = coverText.subtitle,
                isVisible = true,
                xFraction = 0.5f,
                yFraction = 0.48f,
                fontSize = style.titleFontSize * 0.6f,
                colorHex = style.titleColorHex,
                isBold = false,
                hasShadow = true,
                hasBackground = false,
                backgroundColorHex = "#00000000",
                textAlign = TextAlignOption.CENTER,
                fontFamily = FontFamilyOption.DEFAULT,
            )
        }
        return fields
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    /**
     * 若 [uri] 是 content:// scheme，通过 ContentResolver 复制到临时文件。
     * 若已是 file:// scheme，直接返回 File 对象。
     */
    private suspend fun uriToFile(uri: Uri, tempDir: File): File = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") return@withContext File(uri.path!!)
        val dest = tempDir.resolve("input_${UUID.randomUUID()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        dest
    }

    private fun uriToFileCached(uri: Uri): File? =
        if (uri.scheme == "file") File(uri.path!!) else null
}
