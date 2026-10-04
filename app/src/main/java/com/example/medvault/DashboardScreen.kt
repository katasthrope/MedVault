package com.example.medvault

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ChartType(val label: String) { LINE("Line"), DOTS("Dots"), HEATMAP("Heatmap") }

private val latestFormat = SimpleDateFormat("EEE, dd MMM yyyy · HH:mm", Locale.getDefault())
private val dayLabelFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

private val LowBlue = Color(0xFF1E88E5)
private val HeatGreen = Color(0xFF43A047)

private fun r1(v: Double) = Math.round(v * 10) / 10.0

private data class Stats(
    val min: Double, val minTs: Long,
    val max: Double, val maxTs: Long,
    val avg: Double
)

private fun statsFor(m: Metric, list: List<LogEntry>, h: Double?): Stats? {
    val pts = list.mapNotNull { e -> m.readingOf(e, h)?.let { e.timestamp to it } }
    if (pts.isEmpty()) return null
    val mn = pts.minBy { it.second }
    val mx = pts.maxBy { it.second }
    return Stats(mn.second, mn.first, mx.second, mx.first, pts.map { it.second }.average())
}

// ---------- Latest log ----------
@Composable
private fun LatestCard(e: LogEntry, t: Thresholds, h: Double?, tagColors: Map<String, Int>) {
    SectionCard {
        SectionTitle("Latest log")
        Text(
            latestFormat.format(Date(e.timestamp)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        ReadingTiles(e, t, h)
        if (e.tag != null) TagPill(e.tag, tagColors[e.tag] ?: TagPalette.FALLBACK)
        if (e.remark.isNotBlank()) {
            Text(e.remark, style = MaterialTheme.typography.bodyMedium, color = mutedColor())
        }
    }
}

// ---------- Min / Avg / Max ----------
@Composable
private fun StatCell(text: String?, ts: Long?, out: Boolean, modifier: Modifier) {
    Column(modifier) {
        if (text != null) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (out) FontWeight.Bold else FontWeight.SemiBold,
                color = if (out) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text("–", style = MaterialTheme.typography.titleMedium, color = mutedColor())
        }
        if (ts != null) {
            Text(
                dayLabelFormat.format(Date(ts)),
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor()
            )
        }
    }
}

@Composable
private fun StatsCard(stats: Map<Metric, Stats?>, t: Thresholds) {
    val line = MaterialTheme.colorScheme.outlineVariant
    SectionCard {
        SectionTitle("Min · Average · Max")
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1.2f))
            listOf("Min", "Avg", "Max").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = mutedColor(), modifier = Modifier.weight(1f))
            }
        }
        Metric.values().forEachIndexed { i, m ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(line))
            val s = stats[m]
            val range = m.rangeOf(t)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.2f)) {
                    Text(m.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(m.unit, style = MaterialTheme.typography.labelSmall, color = mutedColor())
                }
                StatCell(s?.let { m.display(it.min) }, s?.minTs, s?.let { range.isOut(it.min) } ?: false, Modifier.weight(1f))
                StatCell(s?.let { m.display(r1(it.avg)) }, null, s?.let { range.isOut(it.avg) } ?: false, Modifier.weight(1f))
                StatCell(s?.let { m.display(it.max) }, s?.maxTs, s?.let { range.isOut(it.max) } ?: false, Modifier.weight(1f))
            }
        }
    }
}

// ---------- Line / dot chart ----------
@Composable
private fun LineDotChart(points: List<Pair<Long, Double>>, range: Range, line: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = mutedColor()

    var yMin = points.minOf { it.second }
    var yMax = points.maxOf { it.second }
    range.low?.let { yMin = minOf(yMin, it) }
    range.high?.let { yMax = maxOf(yMax, it) }
    if (yMax - yMin < 1e-6) {
        yMin -= 1.0
        yMax += 1.0
    }
    val tMin = points.first().first
    val tMax = points.last().first
    val axis = { v: Double -> fmt(r1(v)) }

    Row(Modifier.fillMaxWidth().height(220.dp)) {
        Column(
            Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(axis(yMax), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(axis((yMax + yMin) / 2), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(axis(yMin), style = MaterialTheme.typography.labelSmall, color = muted)
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            val inset = 10.dp.toPx()
            fun xOf(t: Long): Float =
                if (tMax == tMin) w / 2f
                else inset + ((t - tMin).toFloat() / (tMax - tMin).toFloat()) * (w - 2 * inset)
            fun yOf(v: Double): Float =
                (inset + (1.0 - (v - yMin) / (yMax - yMin)) * (h - 2 * inset)).toFloat()

            for (i in 0..3) {
                val y = inset + (h - 2 * inset) * i / 3f
                drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            range.high?.let {
                drawLine(error.copy(alpha = 0.85f), Offset(0f, yOf(it)), Offset(w, yOf(it)), strokeWidth = 2.dp.toPx(), pathEffect = dash)
            }
            range.low?.let {
                drawLine(LowBlue, Offset(0f, yOf(it)), Offset(w, yOf(it)), strokeWidth = 2.dp.toPx(), pathEffect = dash)
            }

            if (line && points.size > 1) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = xOf(p.first)
                    val y = yOf(p.second)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path, primary,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            val r = if (line) 3.5.dp.toPx() else 5.dp.toPx()
            points.forEach { p ->
                drawCircle(
                    if (range.isOut(p.second)) error else primary,
                    r, Offset(xOf(p.first), yOf(p.second))
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text(dayLabelFormat.format(Date(tMin)), style = MaterialTheme.typography.labelSmall, color = muted)
        Spacer(Modifier.weight(1f))
        Text(dayLabelFormat.format(Date(tMax)), style = MaterialTheme.typography.labelSmall, color = muted)
    }
    if (range.low != null || range.high != null) {
        Text(
            "Red dots are outside your thresholds. Dashed lines: red = High, blue = Low.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
    }
}

// ---------- Heatmap ----------
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = mutedColor())
    }
}

@Composable
private fun Heatmap(
    entries: List<LogEntry>,
    metric: Metric,
    heightCm: Double?,
    range: Range,
    f: FilterState
) {
    val noData = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val muted = mutedColor()

    val now = System.currentTimeMillis()
    val dd = f.durationDays
    val rf = f.rangeFrom
    val rt = f.rangeTo
    val end = startOfDay(if (dd != null || rt == null) now else rt)
    var start = when {
        dd != null -> startOfDay(now, dd - 1)
        rf != null -> startOfDay(rf)
        else -> startOfDay(entries.minOfOrNull { it.timestamp } ?: now)
    }
    val earliest = startOfDay(end, 181)   // show at most 26 weeks
    if (start < earliest) start = earliest
    if (start > end) start = end

    val dayStats = remember(entries, metric, heightCm) {
        val map = HashMap<Long, DoubleArray>()
        entries.forEach { e ->
            metric.readingOf(e, heightCm)?.let { v ->
                val k = startOfDay(e.timestamp)
                val a = map[k]
                if (a == null) map[k] = doubleArrayOf(v, v)
                else {
                    if (v < a[0]) a[0] = v
                    if (v > a[1]) a[1] = v
                }
            }
        }
        map
    }

    val gridStart = run {
        val c = Calendar.getInstance()
        c.timeInMillis = start
        startOfDay(start, (c.get(Calendar.DAY_OF_WEEK) + 5) % 7) // back to Monday
    }
    val days = ArrayList<Long>()
    val cal = Calendar.getInstance().apply { timeInMillis = gridStart }
    while (cal.timeInMillis <= end) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    val cols = (days.size + 6) / 7

    val vals = days.mapNotNull { d -> if (d >= start) dayStats[d]?.get(1) else null }
    val vMin = vals.minOrNull() ?: 0.0
    val vMax = vals.maxOrNull() ?: 1.0
    val hasRange = range.low != null || range.high != null
    val hi = range.high
    val lo = range.low

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 3.dp
        val cell = minOf((maxWidth - gap * (cols - 1)) / cols, 26.dp)
        val gridW = cell * cols + gap * (cols - 1)
        val gridH = cell * 7 + gap * 6
        Canvas(Modifier.width(gridW).height(gridH)) {
            val c = cell.toPx()
            val g = gap.toPx()
            val radius = CornerRadius(4.dp.toPx())
            days.forEachIndexed { i, dayMs ->
                if (dayMs < start) return@forEachIndexed
                val st = dayStats[dayMs]
                val color = when {
                    st == null -> noData
                    hi != null && st[1] > hi -> error
                    lo != null && st[0] < lo -> LowBlue
                    hasRange -> HeatGreen
                    else -> {
                        val t = if (vMax - vMin < 1e-9) 1.0 else (st[1] - vMin) / (vMax - vMin)
                        primary.copy(alpha = (0.25 + 0.75 * t).toFloat())
                    }
                }
                drawRoundRect(
                    color,
                    Offset((i / 7) * (c + g), (i % 7) * (c + g)),
                    Size(c, c),
                    radius
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Text(dayLabelFormat.format(Date(start)), style = MaterialTheme.typography.labelSmall, color = muted)
        Spacer(Modifier.weight(1f))
        Text(dayLabelFormat.format(Date(end)), style = MaterialTheme.typography.labelSmall, color = muted)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hasRange) {
            if (lo != null) LegendItem(LowBlue, "Below")
            LegendItem(HeatGreen, "In range")
            if (hi != null) LegendItem(error, "Above")
        } else {
            LegendItem(primary, "Darker = higher")
        }
        LegendItem(noData, "No data")
    }
    Text(
        "Each square is one day (columns are weeks, top row is Monday).",
        style = MaterialTheme.typography.bodySmall,
        color = muted
    )
}

// ---------- Trends card ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendCard(
    filtered: List<LogEntry>,
    thresholds: Thresholds,
    heightCm: Double?,
    f: FilterState
) {
    var metric by remember { mutableStateOf(Metric.GLUCOSE) }
    var type by remember { mutableStateOf(ChartType.LINE) }

    val points = remember(filtered, metric, heightCm) {
        filtered.mapNotNull { e -> metric.readingOf(e, heightCm)?.let { e.timestamp to it } }
            .sortedBy { it.first }
    }
    val range = metric.rangeOf(thresholds)

    SectionCard {
        SectionTitle("Trends")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Metric.values().forEach { m -> TagChip(m.label, null, metric == m) { metric = m } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartType.values().forEach { t -> TagChip(t.label, null, type == t) { type = t } }
        }
        Text(
            "${metric.label} (${metric.unit})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        if (points.isEmpty()) {
            Text(
                "No ${metric.label.lowercase()} readings in this selection.",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor()
            )
        } else if (type == ChartType.HEATMAP) {
            Heatmap(filtered, metric, heightCm, range, f)
        } else {
            LineDotChart(points, range, line = type == ChartType.LINE)
        }
    }
}

@Composable
fun DashboardScreen(db: HealthDb, onBack: () -> Unit) {
    val context = LocalContext.current
    val thresholds = remember { ThresholdStore.load(context) }
    val heightCm = remember { ProfileStore.load(context).heightCm }
    val tagColors = remember { TagStore.colorMap(context) }
    val entries = remember { db.getAll() } // newest -> oldest
    val f = remember { FilterState() }
    var showFilters by remember { mutableStateOf(false) }

    val filterTags = remember(entries) { tagsForFilter(context, entries) }
    val filtered = remember(entries, f.tag, f.durationDays, f.rangeFrom, f.rangeTo) { f.apply(entries) }
    val stats = remember(filtered, heightCm) {
        Metric.values().associateWith { statsFor(it, filtered, heightCm) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Dashboard", onBack)

        if (entries.isEmpty()) {
            EmptyState("No data yet", "Add logs, or use Populate sample in Settings.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "filterHeader") {
                    FilterHeader(f, showFilters, { showFilters = !showFilters }, filtered.size, entries.size)
                }
                if (showFilters) {
                    item(key = "filters") { FilterPanel(f, filterTags) }
                }
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            "No records match",
                            "Try a different tag or date range.",
                            action = { SecondaryButton("Clear filters", onClick = { f.clear() }) }
                        )
                    }
                } else {
                    item(key = "latest") { LatestCard(filtered.first(), thresholds, heightCm, tagColors) }
                    item(key = "stats") { StatsCard(stats, thresholds) }
                    item(key = "trend") { TrendCard(filtered, thresholds, heightCm, f) }
                }
            }
        }
    }
}