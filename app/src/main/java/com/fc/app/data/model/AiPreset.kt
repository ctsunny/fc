package com.fc.app.data.model

import kotlinx.serialization.Serializable

/** 字幕语言选项 */
@Serializable
enum class SubtitleLang(val label: String) {
    ZH("中文"),
    ZH_EN("中英双语"),
}

/** AI 工作流类型 */
@Serializable
enum class AiWorkflow(val label: String) {
    SUBTITLE_ONLY("仅生成字幕"),
    COVER_AND_SUBTITLE("封面 + 字幕"),
    FULL_AUTO("全自动（封面 + 字幕 + 优化）"),
}

/** AI 服务商 */
@Serializable
enum class AiProvider(val label: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com", "gpt-4o-mini"),
    QWEN("阿里云百炼", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-turbo"),
    ZHIPU("智谱 AI", "https://open.bigmodel.cn/api/paas", "glm-4-flash"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
    CUSTOM("自定义", "", ""),
}

/** 字幕单条样式（与 OverlayTextField 解耦，只存样式参数） */
@Serializable
data class CaptionStyle(
    val fontFamily: FontFamilyOption = FontFamilyOption.DEFAULT,
    val fontSize: Float = 20f,
    val colorHex: String = "#FFFFFF",
    val isBold: Boolean = false,
    val hasShadow: Boolean = true,
    val hasBackground: Boolean = true,
    val backgroundColorHex: String = "#AA000000",
    /** 字幕纵向位置（0.0 顶部 ~ 1.0 底部） */
    val yFraction: Float = 0.82f,
)

/** 封面样式 */
@Serializable
data class CoverStyle(
    /** 封面截帧位置比例（0.0 = 视频开头，1.0 = 视频结尾） */
    val framePositionFraction: Float = 0.1f,
    /** 封面大标题最大字数 */
    val maxTitleChars: Int = 12,
    /** 封面副标题最大字数 */
    val maxSubtitleChars: Int = 20,
    val titleFontSize: Float = 36f,
    val titleColorHex: String = "#FFFFFF",
    val titleBgColorHex: String = "#CC000000",
    /** 封面叠加持续时长（秒） */
    val overlayDurationSecs: Int = 3,
)

/**
 * AI 编辑预设，每条预设保存一套完整的工作流 + 模型 + 样式配置。
 * 序列化后存入 SharedPreferences。
 */
@Serializable
data class AiPreset(
    val id: String,
    val name: String,
    val workflow: AiWorkflow = AiWorkflow.COVER_AND_SUBTITLE,
    val captionStyle: CaptionStyle = CaptionStyle(),
    val coverStyle: CoverStyle = CoverStyle(),
    /** 覆盖全局 AI 设置，使用预设专属的服务商 */
    val apiProvider: AiProvider = AiProvider.QWEN,
    /** 覆盖全局 AI 设置，使用预设专属的模型 */
    val modelId: String = AiProvider.QWEN.defaultModel,
    /** 单次 LLM 调用 token 上限（0 = 使用全局设置） */
    val maxTokenBudget: Int = 0,
    /** 字幕语言（0 = 使用全局设置） */
    val subtitleLang: SubtitleLang = SubtitleLang.ZH,
    /** 品名（用于封面文案提示词） */
    val productName: String = "",
    /** 价格文字（用于封面文案提示词） */
    val priceText: String = "",
)
