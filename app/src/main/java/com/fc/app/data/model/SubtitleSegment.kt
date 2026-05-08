package com.fc.app.data.model

import kotlinx.serialization.Serializable

/**
 * 单条字幕片段，带时间戳。
 * [startMs] 和 [endMs] 为视频毫秒偏移量。
 */
@Serializable
data class SubtitleSegment(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)
