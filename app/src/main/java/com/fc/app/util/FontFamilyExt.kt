package com.fc.app.util

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.fc.app.data.model.FontFamilyOption

/** 将 [FontFamilyOption] 映射为 Compose [FontFamily]（用于预览画布）。 */
fun FontFamilyOption.toComposeFontFamily(): FontFamily = when (this) {
    FontFamilyOption.DEFAULT -> FontFamily.Default
    FontFamilyOption.SANS_SERIF_MEDIUM -> FontFamily.SansSerif
    FontFamilyOption.CONDENSED -> FontFamily.SansSerif
    FontFamilyOption.SERIF -> FontFamily.Serif
}

/** 将 [FontFamilyOption] 映射为 Android [Typeface]（用于导出位图）。 */
fun FontFamilyOption.toTypeface(bold: Boolean): Typeface {
    val style = if (bold) Typeface.BOLD else Typeface.NORMAL
    return when (this) {
        FontFamilyOption.DEFAULT -> if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        FontFamilyOption.SANS_SERIF_MEDIUM -> Typeface.create("sans-serif-medium", style)
        FontFamilyOption.CONDENSED -> Typeface.create("sans-serif-condensed", style)
        FontFamilyOption.SERIF -> Typeface.create("serif", style)
    }
}
