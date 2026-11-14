package de.smartfinance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.smartfinance.app.domain.DayLoadPoint
import de.smartfinance.app.domain.SmartFinanceEngine
import kotlin.math.ln
import kotlin.math.max

/**
 * Grafische Monats-Belastungswelle (Jetpack Compose Canvas Bar/Wave-Chart).
 * Visualisiert die Tage 1 bis 31 des Monats, hebt hohe Abbuchungsspitzen (Peaks)
 * farblich hervor und trennt per vertikaler "Heute"-Markierungslinie bereits
 * erledigte Abbuchungen (links) von noch kommenden Lastschriften (rechts).
 */
@Composable
fun CashflowWaveChart(
    dayPoints: List<DayLoadPoint>,
    loadStartThird: Double,
    loadMiddleThird: Double,
    loadEndThird: Double,
    modifier: Modifier = Modifier
) {
    if (dayPoints.isEmpty()) return

    var selectedDayPoint by remember(dayPoints) {
        mutableStateOf(
            dayPoints.firstOrNull { it.isToday }
                ?: dayPoints.maxByOrNull { it.totalLoad }
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val settledColor = Color(0xFF10B981) // Smaragdgrün (erledigt)
    val upcomingColor = Color(0xFF38BDF8) // Cyanblau (offen)
    val peakColor = Color(0xFFF59E0B) // Bernstein/Orange (Belastungs-Peak)
    val todayLineColor = Color(0xFFEF4444) // Korallenrot (Heute-Trennlinie)
    val waveStrokeColor = MaterialTheme.colorScheme.primary
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Legende (Erledigt | Offen | Peak | Heute-Linie)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = settledColor, label = "Erledigt (Links)")
            LegendDot(color = upcomingColor, label = "Offen (Rechts)")
            LegendDot(color = peakColor, label = "Peak-Spitze")
            LegendDot(color = todayLineColor, label = "Heute-Linie")
        }

        // Canvas Bar + Smooth Wave Chart (Tag 1 bis 31)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .pointerInput(dayPoints) {
                        detectTapGestures { tapOffset ->
                            val count = dayPoints.size
                            if (count > 0 && size.width > 0) {
                                val slotWidth = size.width.toFloat() / count.toFloat()
                                val index = (tapOffset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                selectedDayPoint = dayPoints[index]
                            }
                        }
                    }
            ) {
                val count = dayPoints.size
                val chartBottom = size.height - 24.dp.toPx()
                val chartTop = 18.dp.toPx()
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)
                val slotWidth = size.width / count.toFloat()

                // Logarithmisch gedämpfte Skalierung, damit sowohl 30-€-Peaks (Handy) als auch 1.100-€-Peaks (Miete) klar sichtbar sind
                val maxScaledLoad = dayPoints.maxOfOrNull { scaledValue(it.totalLoad) }?.coerceAtLeast(1f) ?: 1f

                // 1. Drittel-Trennlinien (Tag 10 & Tag 20)
                listOf(10, 20).forEach { dividerDay ->
                    if (dividerDay < count) {
                        val x = dividerDay * slotWidth
                        drawLine(
                            color = labelTextColor.copy(alpha = 0.2f),
                            start = Offset(x, chartTop),
                            end = Offset(x, chartBottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }

                // 2. Wellen-Pfad (Smooth Bézier Curve) über alle 31 Tage berechnen
                val wavePath = Path()
                val fillPath = Path()
                val pointCoords = dayPoints.mapIndexed { idx, pt ->
                    val cx = idx * slotWidth + slotWidth / 2f
                    val ratio = (scaledValue(pt.totalLoad) / maxScaledLoad).coerceIn(0f, 1f)
                    val cy = chartBottom - ratio * chartHeight
                    Offset(cx, cy)
                }

                if (pointCoords.isNotEmpty()) {
                    wavePath.moveTo(pointCoords.first().x, pointCoords.first().y)
                    fillPath.moveTo(pointCoords.first().x, chartBottom)
                    fillPath.lineTo(pointCoords.first().x, pointCoords.first().y)

                    for (i in 1 until pointCoords.size) {
                        val prev = pointCoords[i - 1]
                        val curr = pointCoords[i]
                        val controlX = (prev.x + curr.x) / 2f
                        wavePath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                        fillPath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                    }

                    fillPath.lineTo(pointCoords.last().x, chartBottom)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                waveStrokeColor.copy(alpha = 0.25f),
                                waveStrokeColor.copy(alpha = 0.02f)
                            ),
                            startY = chartTop,
                            endY = chartBottom
                        )
                    )

                    drawPath(
                        path = wavePath,
                        color = waveStrokeColor.copy(alpha = 0.7f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. Tages-Balken & Peak-Marker zeichnen
                dayPoints.forEachIndexed { idx, pt ->
                    val cx = idx * slotWidth + slotWidth / 2f
                    val ratio = (scaledValue(pt.totalLoad) / maxScaledLoad).coerceIn(0f, 1f)
                    val barHeight = if (pt.totalLoad > 0.0) {
                        max(ratio * chartHeight, 6.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                    val barWidth = (slotWidth * 0.62f).coerceAtLeast(3.dp.toPx())
                    val barTop = chartBottom - barHeight

                    val barColor = when {
                        pt.totalLoad <= 0.0 -> labelTextColor.copy(alpha = 0.18f)
                        pt.isPeak && !pt.isPastOrToday -> peakColor
                        pt.isPeak && pt.isPastOrToday -> Color(0xFFD97706)
                        pt.isPastOrToday -> settledColor
                        else -> upcomingColor
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - barWidth / 2f, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Hervorhebung für den angetippten Tag
                    if (selectedDayPoint?.dayOfMonth == pt.dayOfMonth) {
                        drawCircle(
                            color = barColor,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, barTop - 5.dp.toPx())
                        )
                    }

                    // X-Achsen-Tageslabels (1., 5., 10., 15., 20., 25., 28., Monatsende)
                    if (pt.dayOfMonth in listOf(1, 5, 10, 15, 20, 25, 28, count)) {
                        val textLayout = textMeasurer.measure(
                            text = "${pt.dayOfMonth}.",
                            style = TextStyle(
                                color = if (pt.isToday) todayLineColor else labelTextColor,
                                fontSize = 9.sp,
                                fontWeight = if (pt.isToday || pt.isPeak) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                (cx - textLayout.size.width / 2f).coerceIn(0f, size.width - textLayout.size.width),
                                chartBottom + 4.dp.toPx()
                            )
                        )
                    }
                }

                // 4. Vertikale "HEUTE"-Markierungslinie
                val todayIdx = dayPoints.indexOfFirst { it.isToday }
                if (todayIdx >= 0) {
                    val todayX = todayIdx * slotWidth + slotWidth / 2f
                    drawLine(
                        color = todayLineColor,
                        start = Offset(todayX, 2.dp.toPx()),
                        end = Offset(todayX, chartBottom + 2.dp.toPx()),
                        strokeWidth = 2.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                    val badgeText = textMeasurer.measure(
                        text = "HEUTE (${dayPoints[todayIdx].dayOfMonth}.)",
                        style = TextStyle(
                            color = todayLineColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    val labelX = (todayX - badgeText.size.width / 2f)
                        .coerceIn(2f, size.width - badgeText.size.width - 2f)
                    drawText(
                        textLayoutResult = badgeText,
                        topLeft = Offset(labelX, 0f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monatsdrittel-Auswertung (Anfang 1–10 | Mitte 11–20 | Ende 21–31)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MonthThirdChip(
                title = "Anfang (1.–10.)",
                amount = loadStartThird,
                isHighest = loadStartThird >= loadMiddleThird && loadStartThird >= loadEndThird,
                modifier = Modifier.weight(1f)
            )
            MonthThirdChip(
                title = "Mitte (11.–20.)",
                amount = loadMiddleThird,
                isHighest = loadMiddleThird > loadStartThird && loadMiddleThird >= loadEndThird,
                modifier = Modifier.weight(1f)
            )
            MonthThirdChip(
                title = "Ende (21.–31.)",
                amount = loadEndThird,
                isHighest = loadEndThird > loadStartThird && loadEndThird > loadMiddleThird,
                modifier = Modifier.weight(1f)
            )
        }

        // Detail-Anzeige für den ausgewählten Tag beim Antippen im Diagramm
        selectedDayPoint?.let { pt ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val statusTag = when {
                        pt.isToday -> "📍 Heute (Tag ${pt.dayOfMonth})"
                        pt.isPastOrToday -> "✅ Tag ${pt.dayOfMonth} (Bereits gebucht)"
                        else -> "⏳ Tag ${pt.dayOfMonth} (Geplante Abbuchung)"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusTag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tagessumme: ${SmartFinanceEngine.formatEuro(pt.totalLoad)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (pt.items.isNotEmpty()) {
                        Text(
                            text = pt.items.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Keine Abbuchungen an diesem Kalendertag (Tippe auf einen Balken im Diagramm für Details).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun scaledValue(amount: Double): Float {
    if (amount <= 0.0) return 0f
    return ln(1.0 + amount).toFloat()
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthThirdChip(
    title: String,
    amount: Double,
    isHighest: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isHighest && amount > 0.0) {
        Color(0xFFF59E0B).copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isHighest && amount > 0.0) "🔥 $title" else title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = SmartFinanceEngine.formatEuro(amount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighest) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isHighest && amount > 0.0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
