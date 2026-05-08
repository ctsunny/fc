package com.fc.app.service

import android.util.Log
import com.fc.app.data.model.CoverText
import com.fc.app.data.model.SubtitleSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 封装对 OpenAI 兼容格式 API 的调用。
 * 支持：
 *  - POST /v1/audio/transcriptions  → 语音转文字（Whisper）
 *  - POST /v1/chat/completions      → 字幕优化 + 封面文案（合并为一次调用）
 */
class AiApiClient(
    /** API Base URL，例如 "https://api.openai.com" 或自定义中转地址 */
    private val baseUrl: String,
    /** Bearer token */
    private val apiKey: String,
    /** 用于 chat/completions 的模型 ID */
    private val modelId: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Whisper 转录 ─────────────────────────────────────────────────────────

    /**
     * 调用 Whisper 接口，返回带时间戳的字幕片段列表。
     * 若接口不支持 verbose_json，则回退为仅返回全文（单条片段）。
     */
    suspend fun transcribe(
        audioFile: File,
        language: String = "zh",
    ): List<SubtitleSegment> = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}/v1/audio/transcriptions"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType()),
            )
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("language", language)
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("timestamp_granularities[]", "segment")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val responseBody = executeRequest(request)

        // 尝试解析 verbose_json 格式（带 segments）
        runCatching {
            val verbose = json.decodeFromString<WhisperVerboseResponse>(responseBody)
            verbose.segments.mapIndexed { idx, seg ->
                SubtitleSegment(
                    id = "seg_$idx",
                    startMs = (seg.start * 1000).toLong(),
                    endMs = (seg.end * 1000).toLong(),
                    text = seg.text.trim(),
                )
            }
        }.getOrElse {
            // 降级：解析普通 text 格式
            runCatching {
                val simple = json.decodeFromString<WhisperSimpleResponse>(responseBody)
                listOf(SubtitleSegment("seg_0", 0L, 0L, simple.text.trim()))
            }.getOrElse {
                Log.e(TAG, "Failed to parse transcription response: $responseBody")
                emptyList()
            }
        }
    }

    // ─── 字幕优化 + 封面文案（合并一次调用） ──────────────────────────────────

    /**
     * 将 ASR 纯文本发给 LLM，同时完成：
     *  1. 错字纠正 + 营销语气美化（字幕文本）
     *  2. 封面大标题 + 副标题生成（若 [withCover] = true）
     *
     * Token 节省：
     *  - 仅发纯文字（不含时间戳）
     *  - 极短 system prompt
     *  - 强制 json_object 输出格式
     *  - 输入文本若超过 [maxInputChars] 自动截断
     *
     * @return [LlmEditResult]，调用方负责将优化后的文本按字符比例重新映射回时间戳
     */
    suspend fun refineSubtitlesAndCover(
        asrText: String,
        productName: String = "",
        priceText: String = "",
        maxTokenBudget: Int = 2000,
        withCover: Boolean = true,
        subtitleLang: String = "zh",
    ): LlmEditResult = withContext(Dispatchers.IO) {
        val maxInputChars = (maxTokenBudget * INPUT_TOKEN_RATIO * CHINESE_CHARS_PER_TOKEN).toInt()
        val truncatedText = if (asrText.length > maxInputChars) {
            asrText.take(maxInputChars) + "…"
        } else {
            asrText
        }

        val systemPrompt = if (subtitleLang == "zh") SYSTEM_PROMPT_ZH else SYSTEM_PROMPT_ZH_EN

        val coverInstruction = if (withCover && productName.isNotBlank()) {
            "\n商品:$productName${if (priceText.isNotBlank()) " 价格:$priceText" else ""}。生成封面title(≤12字)和subtitle(≤20字)。"
        } else if (withCover) {
            "\n根据内容生成封面title(≤12字)和subtitle(≤20字)。"
        } else ""

        val userPrompt = """原始字幕文本：
$truncatedText$coverInstruction
请输出JSON格式：{"refined":"优化后的完整文本","title":"封面标题","subtitle":"封面副标题"}"""

        val requestJson = buildChatRequest(
            model = modelId,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            maxTokens = maxTokenBudget,
            jsonMode = true,
        )

        val url = "${baseUrl.trimEnd('/')}/v1/chat/completions"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val responseBody = executeRequest(request)
        parseLlmResult(responseBody, asrText)
    }

    // ─── 私有辅助 ──────────────────────────────────────────────────────────────

    private fun executeRequest(request: Request): String {
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw AiApiException(response.code, body)
        }
        return body
    }

    private fun buildChatRequest(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        jsonMode: Boolean,
    ): String {
        val responseFormat = if (jsonMode) ""","response_format":{"type":"json_object"}""" else ""
        return """{"model":"$model","messages":[{"role":"system","content":${escapeJson(systemPrompt)}},{"role":"user","content":${escapeJson(userPrompt)}}],"max_tokens":$maxTokens$responseFormat}"""
    }

    private fun parseLlmResult(responseBody: String, fallbackText: String): LlmEditResult {
        return runCatching {
            val completion = json.decodeFromString<ChatCompletionResponse>(responseBody)
            val content = completion.choices.firstOrNull()?.message?.content ?: return LlmEditResult(fallbackText, CoverText())
            val parsed = json.decodeFromString<LlmJsonOutput>(content)
            LlmEditResult(
                refinedText = parsed.refined.ifBlank { fallbackText },
                coverText = CoverText(title = parsed.title, subtitle = parsed.subtitle),
            )
        }.getOrElse { e ->
            Log.e(TAG, "LLM result parse error, using fallback: $e")
            LlmEditResult(fallbackText, CoverText())
        }
    }

    private fun escapeJson(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")}\""

    companion object {
        private const val TAG = "AiApiClient"
        /** 占用 token 预算的输入文本比例上限。 */
        private const val INPUT_TOKEN_RATIO = 0.6
        /** 中文字符与 token 的近似换算系数（每个 token ≈ 1.5 汉字）。 */
        private const val CHINESE_CHARS_PER_TOKEN = 1.5
        private const val SYSTEM_PROMPT_ZH = "纠错并美化为营销口吻的中文，JSON输出"
        private const val SYSTEM_PROMPT_ZH_EN = "纠错并美化为营销口吻，保留中英双语，JSON输出"
    }

    // ─── 内部序列化数据类 ─────────────────────────────────────────────────────

    @Serializable
    private data class WhisperVerboseResponse(val segments: List<WhisperSegment> = emptyList())

    @Serializable
    private data class WhisperSegment(val start: Float = 0f, val end: Float = 0f, val text: String = "")

    @Serializable
    private data class WhisperSimpleResponse(val text: String = "")

    @Serializable
    private data class ChatCompletionResponse(val choices: List<ChatChoice> = emptyList())

    @Serializable
    private data class ChatChoice(val message: ChatMessage = ChatMessage())

    @Serializable
    private data class ChatMessage(val content: String = "")

    @Serializable
    private data class LlmJsonOutput(
        val refined: String = "",
        val title: String = "",
        val subtitle: String = "",
    )
}

/** LLM 编辑结果 */
data class LlmEditResult(
    val refinedText: String,
    val coverText: CoverText,
)

/** API 调用失败时抛出此异常 */
class AiApiException(val code: Int, val body: String) : Exception("AI API error $code: $body")
