package com.fc.app.data.model

import kotlinx.serialization.Serializable

/**
 * 封面文案，由 LLM 生成后供用户复核修改。
 */
@Serializable
data class CoverText(
    val title: String = "",
    val subtitle: String = "",
)
