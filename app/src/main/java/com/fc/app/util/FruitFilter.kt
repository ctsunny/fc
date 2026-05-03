package com.fc.app.util

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment

/**
 * 大师级水果拍摄专用视频滤镜
 *
 * 两种滤镜均经过专业调色设计，适用于水果类商品的视频拍摄后期处理：
 *  - WARM_FRUIT: 暖果增艳 — 增强暖色调与饱和度，适合草莓、芒果、橙子等暖色水果
 *  - FRESH_FRUIT: 清新翠果 — 增强清新绿调与通透感，适合青苹果、猕猴桃、葡萄等冷色水果
 */
enum class FruitFilter(
    val label: String,
    val description: String,
) {
    WARM_FRUIT(
        label = "暖果增艳",
        description = "暖色调加饱和，增强草莓/芒果/橙子等暖色水果的色彩张力",
    ),
    FRESH_FRUIT(
        label = "清新翠果",
        description = "清新绿调加通透，增强青苹果/猕猴桃/葡萄等冷色水果的质感",
    );

    /**
     * Returns the list of media3 [Effect]s that together realise this filter.
     * The effects are designed to be inserted before the [Presentation] and overlay effects
     * in [VideoExporter] so that colour grading is applied to the raw video signal.
     */
    @OptIn(UnstableApi::class)
    fun buildEffects(): List<Effect> = when (this) {
        WARM_FRUIT -> listOf(
            // +8° hue shift toward orange-red, strong saturation boost, gentle brightness lift
            HslAdjustment.Builder()
                .adjustHue(8f)
                .adjustSaturation(0.55f)
                .adjustLightness(0.06f)
                .build(),
            // Mild contrast to enhance texture detail
            Contrast(0.15f),
        )
        FRESH_FRUIT -> listOf(
            // -10° hue shift toward green-cool, saturation boost, slight darkening for depth
            HslAdjustment.Builder()
                .adjustHue(-10f)
                .adjustSaturation(0.45f)
                .adjustLightness(-0.02f)
                .build(),
            // Stronger contrast for vivid cool clarity
            Contrast(0.25f),
        )
    }
}
