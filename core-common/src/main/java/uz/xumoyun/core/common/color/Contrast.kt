package uz.xumoyun.core.common.color

import kotlin.math.pow

/**
 * WCAG 2.x kontrast nisbati (TZ 13-bo'lim: kontrast tekshiruvi). Ranglar 0xRRGGBB ko'rinishida.
 * Matn uchun minimal nisbat — 4.5:1 (oddiy matn), 3:1 (katta matn va belgilar).
 */
object Contrast {
    const val AA_TEXT = 4.5
    const val AA_LARGE = 3.0

    fun relativeLuminance(rgb: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((rgb shr 16) and 0xFF)
        val g = channel((rgb shr 8) and 0xFF)
        val b = channel(rgb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun ratio(foreground: Int, background: Int): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
