package com.example.medvault

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val DURATIONS = listOf(7, 14, 30, 60, 90)
val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

fun startOfDay(ms: Long, daysBack: Int = 0): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -daysBack)
    return c.timeInMillis
}

fun endOfDay(ms: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
}

class FilterState {
    var tag by mutableStateOf<String?>(null)
    var durationDays by mutableStateOf<Int?>(null)
    var rangeFrom by mutableStateOf<Long?>(null)
    var rangeTo by mutableStateOf<Long?>(null)

    val dateActive: Boolean get() = durationDays != null || rangeFrom != null || rangeTo != null
    val activeCount: Int get() = (if (tag != null) 1 else 0) + (if (dateActive) 1 else 0)

    fun clear() {
        tag = null
        durationDays = null
        rangeFrom = null
        rangeTo = null
    }

    fun apply(entries: List<LogEntry>): List<LogEntry> {
        val dd = durationDays
        val from: Long? = if (dd != null) startOfDay(System.currentTimeMillis(), dd - 1) else rangeFrom
        val to: Long? = if (dd != null) null else rangeTo?.let { endOfDay(it) }
        val t = tag
        return entries.filter { e ->
            (t == null || e.tag == t) &&
                    (from == null || e.timestamp >= from) &&
                    (to == null || e.timestamp <= to)
        }
    }

    fun summary(): String = buildList {
        tag?.let { add("Tag: $it") }
        val dd = durationDays
        if (dd != null) {
            add("Last $dd days")
        } else {
            val f = rangeFrom
            val t = rangeTo
            if (f != null && t != null) {
                add("${shortDateFormat.format(Date(f))} – ${shortDateFormat.format(Date(t))}")
            } else if (f != null) {
                add("From ${shortDateFormat.format(Date(f))}")
            } else if (t != null) {
                add("Until ${shortDateFormat.format(Date(t))}")
            }
        }
    }.joinToString(" · ")
}

/** Known tags plus any tag still used by records. */
fun tagsForFilter(context: Context, entries: List<LogEntry>): List<Tag> {
    val known = TagStore.all(context)
    val extra = entries.mapNotNull { it.tag }.distinct()
        .filter { n -> known.none { it.name == n } }
        .map { Tag(it, TagPalette.FALLBACK) }
    return known + extra
}

@Composable
fun FilterHeader(
    state: FilterState,
    expanded: Boolean,
    onToggle: () -> Unit,
    shown: Int,
    total: Int,
    hint: String? = null
) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = "Filters" +
                    (if (state.activeCount > 0) " (${state.activeCount})" else "") +
                    (if (expanded) "  ▴" else "  ▾")
            TagChip(label, null, state.activeCount > 0 || expanded) { onToggle() }
            Spacer(Modifier.weight(1f))
            Text(
                if (state.activeCount > 0) "$shown of $total records" else "$total records",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor()
            )
        }
        if (state.activeCount > 0 && !expanded) {
            Text(
                state.summary(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (!expanded && hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor(),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(state: FilterState, tags: List<Tag>) {
    val context = LocalContext.current
    val noDateFilter = !state.dateActive

    SectionCard {
        SectionTitle("Tag")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagChip("All", null, state.tag == null) { state.tag = null }
            tags.forEach { t ->
                TagChip(t.name, t.colorIndex, state.tag == t.name) {
                    state.tag = if (state.tag == t.name) null else t.name
                }
            }
        }

        SectionTitle("Duration")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagChip("All time", null, noDateFilter) {
                state.durationDays = null
                state.rangeFrom = null
                state.rangeTo = null
            }
            DURATIONS.forEach { d ->
                TagChip("$d days", null, state.durationDays == d) {
                    state.durationDays = d
                    state.rangeFrom = null
                    state.rangeTo = null
                }
            }
        }

        SectionTitle("Date range")
        val from = state.rangeFrom
        val to = state.rangeTo
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    pickDate(context, from ?: System.currentTimeMillis()) { d ->
                        state.rangeFrom = d
                        state.durationDays = null
                        val t = state.rangeTo
                        if (t != null && t < d) state.rangeTo = d
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (from != null) "From ${shortDateFormat.format(Date(from))}" else "From date", maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    pickDate(context, to ?: System.currentTimeMillis()) { d ->
                        state.rangeTo = d
                        state.durationDays = null
                        val f = state.rangeFrom
                        if (f != null && f > d) state.rangeFrom = d
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (to != null) "To ${shortDateFormat.format(Date(to))}" else "To date", maxLines = 1)
            }
        }
        Text(
            "Choosing a duration clears the date range, and the other way round.",
            style = MaterialTheme.typography.bodySmall,
            color = mutedColor()
        )

        TextButton(onClick = { state.clear() }) {
            Text("Clear all filters", fontWeight = FontWeight.SemiBold)
        }
    }
}