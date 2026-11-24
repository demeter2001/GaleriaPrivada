package com.gallery.privatevault.core.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

object ColorUtils {
    fun parseHexColor(hex: String, defaultColor: Color = Color.Black): Color {
        return try {
            val cleanHex = hex.trim().let {
                if (!it.startsWith("#")) "#$it" else it
            }
            Color(cleanHex.toColorInt())
        } catch (e: Exception) {
            defaultColor
        }
    }

    fun toHexColor(color: Color): String {
        val argb = (color.value.toLong() shr 32).toInt()
        return String.format("#%08X", argb)
    }
}
