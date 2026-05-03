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
    FontFamilyOption.MONOSPACE -> FontFamily.Monospace
    FontFamilyOption.LIGHT -> FontFamily.SansSerif
    FontFamilyOption.BLACK -> FontFamily.SansSerif
    FontFamilyOption.CONDENSED_LIGHT -> FontFamily.SansSerif
}

/** 将 [FontFamilyOption] 映射为 Android [Typeface]（用于导出位图）。 */
fun FontFamilyOption.toTypeface(bold: Boolean): Typeface {
    val style = if (bold) Typeface.BOLD else Typeface.NORMAL
    return when (this) {
        FontFamilyOption.DEFAULT ->
            if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        FontFamilyOption.SANS_SERIF_MEDIUM ->
            Typeface.create("sans-serif-medium", style)
        FontFamilyOption.CONDENSED ->
            Typeface.create("sans-serif-condensed", style)
        FontFamilyOption.SERIF ->
            Typeface.create("serif", style)
        FontFamilyOption.MONOSPACE ->
            Typeface.create("monospace", style)
        FontFamilyOption.LIGHT ->
            // "sans-serif-light" with bold falls back to regular automatically on most devices
            Typeface.create("sans-serif-light", style)
        FontFamilyOption.BLACK ->
            Typeface.create("sans-serif-black", style)
        FontFamilyOption.CONDENSED_LIGHT ->
            Typeface.create("sans-serif-condensed-light", style)
    }
}
