package com.fc.app.util

import android.graphics.Color

fun parseColorOrDefault(value: String, fallback: Int): Int =
    runCatching { Color.parseColor(value) }.getOrDefault(fallback)
