package uz.xumoyun.namoz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import uz.xumoyun.namoz.domain.DayPhase
import kotlin.math.cos
import kotlin.math.sin

/** Kun vaqtiga qarab fon: tong (moviy-pushti), kunduz (och moviy), shom (to'q sariq), tun (to'q ko'k). */
fun phaseGradient(phase: DayPhase): Brush = when (phase) {
    DayPhase.DAWN -> Brush.verticalGradient(listOf(Color(0xFF6A8DD8), Color(0xFFE9A6C0)))
    DayPhase.DAY -> Brush.verticalGradient(listOf(Color(0xFF4FA3E0), Color(0xFFBFE3F7)))
    DayPhase.DUSK -> Brush.verticalGradient(listOf(Color(0xFFE0703A), Color(0xFFF6C177)))
    DayPhase.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF0B1A3A), Color(0xFF1E3A6B)))
}

/** Matn rangi fon ustida o'qiladigan bo'lishi uchun. */
fun phaseContentColor(phase: DayPhase): Color = when (phase) {
    DayPhase.DAY -> Color(0xFF0D2A3F)
    else -> Color.White
}

/**
 * Islomiy geometrik naqsh (sakkiz qirrali yulduzlar setkasi) juda och fon sifatida.
 * Masjid yoki shaxs tasvirlari ishlatilmaydi.
 */
@Composable
fun GeometricPattern(modifier: Modifier = Modifier, color: Color, alpha: Float = 0.07f) {
    Canvas(modifier) {
        val cell = 56.dp.toPx()
        val r = cell * 0.42f
        val stroke = Stroke(width = 1.dp.toPx())
        val c = color.copy(alpha = alpha)
        var y = 0f
        var row = 0
        while (y < size.height + cell) {
            var x = if (row % 2 == 0) 0f else cell / 2
            while (x < size.width + cell) {
                drawPath(star(Offset(x, y), r), c, style = stroke)
                x += cell
            }
            y += cell * 0.866f
            row++
        }
    }
}

private fun star(center: Offset, r: Float): Path = Path().apply {
    val inner = r * 0.55f
    for (i in 0 until 16) {
        val angle = Math.toRadians(i * 22.5 - 90.0)
        val rad = if (i % 2 == 0) r else inner
        val x = center.x + (rad * cos(angle)).toFloat()
        val y = center.y + (rad * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
