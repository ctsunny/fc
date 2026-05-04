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

    // ─────────── 水果促销模板工厂 ───────────
    private const val FRUIT_DEFAULT_ACCENT_COLOR = "#FF3333"
    private const val FRUIT_DEFAULT_TAG_COLOR = "#FF6600"

    private fun fruitTemplate(
        id: String,
        fruitName: String,
        origin: String,
        price: String,
        originalPrice: String,
        slogan: String,
        accentColor: String = FRUIT_DEFAULT_ACCENT_COLOR,
        tagColor: String = FRUIT_DEFAULT_TAG_COLOR,
    ) = StyleTemplate(
        id = id,
        name = "$fruitName 促销",
        category = TemplateCategory.FRUIT,
        fields = listOf(
            OverlayTextField(
                id = "fruit_name",
                label = "水果名称",
                text = fruitName,
                xFraction = 0.05f, yFraction = 0.06f,
                fontSize = 42f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "fruit_origin",
                label = "产地",
                text = "📍 $origin",
                xFraction = 0.05f, yFraction = 0.20f,
                fontSize = 20f, colorHex = "#FFE566",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "fruit_slogan",
                label = "卖点",
                text = slogan,
                xFraction = 0.05f, yFraction = 0.30f,
                fontSize = 22f, colorHex = "#FFFFFF",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "fruit_price",
                label = "活动价",
                text = "¥$price/斤",
                xFraction = 0.05f, yFraction = 0.72f,
                fontSize = 52f, colorHex = accentColor,
                isBold = true, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "fruit_original_price",
                label = "原价",
                text = "原价¥$originalPrice",
                xFraction = 0.05f, yFraction = 0.85f,
                fontSize = 20f, colorHex = "#CCCCCC",
                isBold = false, hasShadow = true,
                textAlign = TextAlignOption.LEFT
            ),
            OverlayTextField(
                id = "fruit_cta",
                label = "立即购买",
                text = "抢购中 >>",
                xFraction = 0.60f, yFraction = 0.85f,
                fontSize = 20f, colorHex = "#FFFFFF",
                isBold = true, hasShadow = false,
                hasBackground = true, backgroundColorHex = tagColor,
                textAlign = TextAlignOption.CENTER
            )
        )
    )

    val fruitTemplates: List<StyleTemplate> = listOf(
        fruitTemplate("fruit_watermelon",   "西瓜",   "海南/新疆", "2.8", "5.0", "清甜多汁 沙瓤爆甜", "#FF3333", "#FF4444"),
        fruitTemplate("fruit_cantaloupe",   "哈密瓜", "新疆哈密",  "4.5", "8.0", "正宗新疆 香甜浓郁", "#FF8C00", "#FF6600"),
        fruitTemplate("fruit_durian",       "榴莲",   "泰国金枕",  "35",  "60",  "D197猫山王 肉厚核小", "#B8860B", "#8B6914"),
        fruitTemplate("fruit_apple",        "苹果",   "山东烟台",  "3.5", "6.0", "脆甜爽口 新鲜采摘", "#CC1111", "#AA0000"),
        fruitTemplate("fruit_banana",       "香蕉",   "广西桂林",  "1.8", "3.5", "自然熟透 香甜软糯", "#FFD700", "#DAA520"),
        fruitTemplate("fruit_orange",       "橙子",   "江西赣南",  "2.5", "5.0", "爆汁脐橙 维C满满", "#FF8C00", "#FF6600"),
        fruitTemplate("fruit_mango",        "芒果",   "云南/海南", "5.5", "10", "小台农芒 香甜无丝", "#FF9900", "#E67E00"),
        fruitTemplate("fruit_grape",        "葡萄",   "新疆吐鲁番","8.0", "15",  "无籽提子 粒粒饱满", "#6B3FA0", "#4B2980"),
        fruitTemplate("fruit_strawberry",   "草莓",   "丹东大棚",  "15",  "28",  "奶油草莓 香甜软嫩", "#FF3366", "#CC1144"),
        fruitTemplate("fruit_peach",        "桃子",   "山东蒙阴",  "3.5", "7.0", "蜜桃上市 汁多味甜", "#FF9966", "#FF7744"),
        fruitTemplate("fruit_pear",         "梨",     "河北鸭梨",  "2.0", "4.0", "清脆多汁 润肺止咳", "#AACC44", "#88AA22"),
        fruitTemplate("fruit_cherry",       "车厘子", "智利进口",  "28",  "55",  "J级车厘子 甜脆无比", "#990000", "#660000"),
        fruitTemplate("fruit_pineapple",    "菠萝",   "广东徐闻",  "3.0", "6.0", "削皮直售 酸甜可口", "#FFD700", "#E6C200"),
        fruitTemplate("fruit_lychee",       "荔枝",   "广东茂名",  "8.0", "15",  "妃子笑荔枝 新鲜上市", "#CC2244", "#AA1133"),
        fruitTemplate("fruit_longan",       "龙眼",   "福建莆田",  "5.0", "10",  "大粒龙眼 清甜爽口", "#CC8833", "#AA6622"),
        fruitTemplate("fruit_pomelo",       "柚子",   "福建平和",  "6.0", "12",  "蜜柚新鲜 酸甜适中", "#DDCC00", "#BBAA00"),
        fruitTemplate("fruit_kiwi",         "猕猴桃", "陕西周至",  "4.0", "8.0", "金果翡翠 维C之王", "#55AA22", "#338800"),
        fruitTemplate("fruit_papaya",       "木瓜",   "海南三亚",  "3.5", "7.0", "红心木瓜 香甜嫩滑", "#FF8844", "#EE6622"),
        fruitTemplate("fruit_plum",         "李子",   "四川眉山",  "4.0", "8.0", "脆甜李子 夏日清爽", "#770088", "#550066"),
        fruitTemplate("fruit_coconut",      "椰子",   "海南文昌",  "6.0", "12",  "新鲜椰青 清凉解渴", "#228866", "#006644"),
        fruitTemplate("fruit_persimmon",    "柿子",   "陕西富平",  "3.0", "6.0", "软糯香甜 柿柿如意", "#FF6600", "#DD4400"),
        fruitTemplate("fruit_pomegranate",  "石榴",   "云南蒙自",  "5.0", "10",  "突尼斯甜石榴 粒大味甜", "#CC2244", "#AA1133"),
        fruitTemplate("fruit_fig",          "无花果", "新疆和田",  "12",  "22",  "新鲜无花果 清甜养颜", "#AA4488", "#884466"),
        fruitTemplate("fruit_blueberry",    "蓝莓",   "云南高原",  "18",  "35",  "鲜食蓝莓 酸甜多汁", "#334488", "#223366"),
        fruitTemplate("fruit_passion",      "百香果", "广西南宁",  "4.0", "8.0", "酸甜百香果 清新多汁", "#AACC22", "#88AA00"),
    )

    val all: List<StyleTemplate> = listOf(ecommerceTemplate, storeTemplate, activityTemplate) + fruitTemplates
}
