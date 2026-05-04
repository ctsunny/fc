package com.fc.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StyleTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val fields: List<OverlayTextField>
)

@Serializable
enum class TemplateCategory {
    ECOMMERCE,  // 电商
    STORE,      // 实体店
    ACTIVITY,   // 活动/促销
    FRUIT       // 水果促销
}
