package com.fc.app.data

import com.fc.app.data.model.*

/**
 * 内置预设：
 *  - promotionFields  导入视频后自动加载的 6 个文字字段
 *  - 三类历史模板（保留备用）
 * 所有坐标均为画布百分比 (xFraction, yFraction)
 */
object PresetTemplates {

    /** 导入视频后自动加载的 6 类文字预设 */
    val promotionFields: List<OverlayTextField> = listOf(
        OverlayTextField(
            id = "product_name",
            label = "产品名",
            text = "产品名",
            xFraction = 0.05f, yFraction = 0.06f,
            fontSize = 36f, colorHex = "#FFFFFF",
            isBold = true, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        ),
        OverlayTextField(
            id = "original_price",
            label = "原价",
            text = "原价",
            xFraction = 0.05f, yFraction = 0.20f,
            fontSize = 24f, colorHex = "#CCCCCC",
            isBold = false, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        ),
        OverlayTextField(
            id = "activity_price",
            label = "活动价",
            text = "活动价",
            xFraction = 0.05f, yFraction = 0.30f,
            fontSize = 32f, colorHex = "#FF3333",
            isBold = true, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        ),
        OverlayTextField(
            id = "remark1",
            label = "备注1",
            text = "备注1",
            xFraction = 0.05f, yFraction = 0.74f,
            fontSize = 20f, colorHex = "#FFFFFF",
            isBold = false, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        ),
        OverlayTextField(
            id = "remark2",
            label = "备注2",
            text = "备注2",
            xFraction = 0.05f, yFraction = 0.83f,
            fontSize = 20f, colorHex = "#FFFFFF",
            isBold = false, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        ),
        OverlayTextField(
            id = "date",
            label = "日期",
            text = "日期",
            xFraction = 0.05f, yFraction = 0.92f,
            fontSize = 18f, colorHex = "#DDDDDD",
            isBold = false, hasShadow = true,
            textAlign = TextAlignOption.LEFT
        )
    )

    // ─────────── 电商爆款 ───────────
    val ecommerceTemplate = StyleTemplate(
        id = "ecommerce_default",
        name = "电商爆款",
        category = TemplateCategory.ECOMMERCE,
        fields = listOf(
            OverlayTextField(
                id = "product_name",
                label = "商品名称",
                text = "爆款新品",
                xFraction = 0.05f, yFraction = 0.07f,
                fontSize = 38f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "price",
                label = "现价",
                text = "¥99",
                xFraction = 0.05f, yFraction = 0.76f,
                fontSize = 52f, colorHex = "#FF3333",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "original_price",
                label = "原价",
                text = "原价¥199",
                xFraction = 0.05f, yFraction = 0.87f,
                fontSize = 20f, colorHex = "#CCCCCC",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "discount_tag",
                label = "折扣标签",
                text = "限时5折",
                xFraction = 0.66f, yFraction = 0.07f,
                fontSize = 22f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = false,
                hasBackground = true, backgroundColorHex = "#FFCC00",
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "cta",
                label = "立即抢购",
                text = "立即抢购 >>",
                xFraction = 0.60f, yFraction = 0.87f,
                fontSize = 20f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = false,
                hasBackground = true, backgroundColorHex = "#FF6600",
                textAlign = TextAlignOption.CENTER
            )
        )
    )

    // ─────────── 实体门店 ───────────
    val storeTemplate = StyleTemplate(
        id = "store_default",
        name = "实体门店",
        category = TemplateCategory.STORE,
        fields = listOf(
            OverlayTextField(
                id = "store_name",
                label = "店铺名称",
                text = "XX 旗舰店",
                xFraction = 0.5f, yFraction = 0.07f,
                fontSize = 40f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "slogan",
                label = "店铺口号",
                text = "品质保证 · 诚信经营",
                xFraction = 0.5f, yFraction = 0.18f,
                fontSize = 20f, colorHex = "#FFE566",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "address",
                label = "地址",
                text = "📍 XX市XX区XX路88号",
                xFraction = 0.05f, yFraction = 0.74f,
                fontSize = 18f, colorHex = "#FFFFFF",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "phone",
                label = "电话",
                text = "📞 400-000-0000",
                xFraction = 0.05f, yFraction = 0.83f,
                fontSize = 18f, colorHex = "#FFFFFF",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "hours",
                label = "营业时间",
                text = "🕙 09:00 – 22:00",
                xFraction = 0.05f, yFraction = 0.91f,
                fontSize = 18f, colorHex = "#FFFFFF",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            )
        )
    )

    // ─────────── 活动促销 ───────────
    val activityTemplate = StyleTemplate(
        id = "activity_default",
        name = "活动促销",
        category = TemplateCategory.ACTIVITY,
        fields = listOf(
            OverlayTextField(
                id = "event_title",
                label = "活动主题",
                text = "年中大促销",
                xFraction = 0.5f, yFraction = 0.09f,
                fontSize = 46f, colorHex = "#FFE100",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "event_subtitle",
                label = "副标题",
                text = "全场低至 1 折起",
                xFraction = 0.5f, yFraction = 0.22f,
                fontSize = 26f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "event_date",
                label = "活动时间",
                text = "6月1日 – 6月30日",
                xFraction = 0.5f, yFraction = 0.67f,
                fontSize = 22f, colorHex = "#FFFFFF",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "event_venue",
                label = "活动地点",
                text = "全国门店 / 线上同步",
                xFraction = 0.5f, yFraction = 0.76f,
                fontSize = 20f, colorHex = "#DDDDDD",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.CENTER
            ),
            OverlayTextField(
                id = "event_cta",
                label = "行动号召",
                text = "立即参与 >>",
                xFraction = 0.5f, yFraction = 0.88f,
                fontSize = 28f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = false,
                hasBackground = true, backgroundColorHex = "#FF3300",
                textAlign = TextAlignOption.CENTER
            )
        )
    )

    val all: List<StyleTemplate> = listOf(ecommerceTemplate, storeTemplate, activityTemplate)
}
