package com.example.medvault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
private val weekdayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
private val dayNumFormat = SimpleDateFormat("dd", Locale.getDefault())
private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

private val DURATIONS = listOf(7, 14, 30, 60, 90)

private fun startOfDay(ms: Long, daysBack: Int = 0): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -daysBack)
    return c.timeInMillis
}

private fun endOfDay(ms: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
}

@Composable
private fun mutedColor() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

@Composable
private fun flagged(out: Boolean): SpanStyle =
    if (out) SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    else SpanStyle(fontWeight = FontWeight.SemiBold)

@Composable
private fun valueText(text: String, out: Boolean): AnnotatedString {
    val style = flagged(out)
    return buildAnnotatedString { withStyle(style) { append(text) } }
}

// ---------- Reading tile (label, value, unit) ----------
@Composable
private fun MetricTile(
    label: String,
    unit: String,
    value: AnnotatedString?,
    out: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (out) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val muted = mutedColor()

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        } else {
            Text("–", style = MaterialTheme.typography.titleMedium, color = muted)
        }
        Text(unit, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
    }
}

@Composable
private fun SelectDot(selected: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) primary else Color.Transparent)
            .border(2.dp, if (selected) primary else mutedColor(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(
                "✓",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------- One record: tap / long-press to select ----------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryBlock(
    e: LogEntry,
    thresholds: Thresholds,
    tagColors: Map<String, Int>,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val muted = mutedColor()

    val gOut = e.glucose?.let { thresholds.glucose.isOut(it) } ?: false
    val uOut = e.uricAcid?.let { thresholds.uric.isOut(it) } ?: false
    val sysOut = e.systolic?.let { thresholds.systolic.isOut(it.toDouble()) } ?: false
    val diaOut = e.diastolic?.let { thresholds.diastolic.isOut(it.toDouble()) } ?: false

    val glucoseText = e.glucose?.let { valueText(fmt(it), gOut) }
    val uricText = e.uricAcid?.let { valueText(fmt(it), uOut) }

    val sysStyle = flagged(sysOut)
    val diaStyle = flagged(diaOut)
    val plainStyle = flagged(false)
    val bpText: AnnotatedString? =
        if (e.systolic != null && e.diastolic != null) {
            buildAnnotatedString {
                withStyle(sysStyle) { append("${e.systolic}") }
                withStyle(plainStyle) { append("/") }
                withStyle(diaStyle) { append("${e.diastolic}") }
            }
        } else null

    val tagIndex: Int? = e.tag?.let { tagColors[it] ?: TagPalette.FALLBACK }
    val barColor =
        if (tagIndex != null) TagPalette.get(tagIndex).base else MaterialTheme.colorScheme.outlineVariant
    val highlight =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(highlight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Colored bar = the record's tag color
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Time left, selection dot right
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clockFormat.format(Date(e.timestamp)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (selecting) SelectDot(selected)
            }

            // Three reading tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Glucose", "mg/dL", glucoseText, gOut, Modifier.weight(1f))
                MetricTile("Uric acid", "mg/dL", uricText, uOut, Modifier.weight(1f))
                MetricTile("Blood pressure", "mmHg", bpText, sysOut || diaOut, Modifier.weight(1.4f))
            }

            // Tag, then remark
            if (e.tag != null) {
                TagPill(e.tag, tagIndex ?: TagPalette.FALLBACK)
            }
            if (e.remark.isNotBlank()) {
                Column {
                    Text("Remark", style = MaterialTheme.typography.labelSmall, color = muted)
                    Text(e.remark, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ---------- One card per day ----------
@Composable
private fun DayCard(
    dayEntries: List<LogEntry>,
    thresholds: Thresholds,
    tagColors: Map<String, Int>,
    selecting: Boolean,
    selectedIds: Set<Long>,
    onClick: (LogEntry) -> Unit,
    onLongClick: (LogEntry) -> Unit
) {
    val date = Date(dayEntries.first().timestamp)
    val line = MaterialTheme.colorScheme.outlineVariant
    val count = dayEntries.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, line)
    ) {
        Column(Modifier.padding(10.dp)) {
            // Day header: date badge + weekday
            Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        dayNumFormat.format(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        monthFormat.format(date).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        weekdayFormat.format(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${yearFormat.format(date)} · " + if (count == 1) "1 record" else "$count records",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor()
                    )
                }
            }

            dayEntries.forEachIndexed { index, e ->
                Spacer(Modifier.height(6.dp))
                if (index > 0) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(1.dp).background(line)
                    )
                    Spacer(Modifier.height(6.dp))
                }
                EntryBlock(
                    e = e,
                    thresholds = thresholds,
                    tagColors = tagColors,
                    selecting = selecting,
                    selected = e.id in selectedIds,
                    onClick = { onClick(e) },
                    onLongClick = { onLongClick(e) }
                )
            }
        }
    }
}

// ---------- Filter panel ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    tags: List<Tag>,
    tagFilter: String?,
    onTag: (String?) -> Unit,
    durationDays: Int?,
    onDuration: (Int) -> Unit,
    rangeFrom: Long?,
    rangeTo: Long?,
    onFrom: (Long) -> Unit,
    onTo: (Long) -> Unit,
    onAllTime: () -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val noDateFilter = durationDays == null && rangeFrom == null && rangeTo == null

    SectionCard {
        SectionTitle("Tag")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagChip("All", null, tagFilter == null) { onTag(null) }
            tags.forEach { t ->
                TagChip(t.name, t.colorIndex, tagFilter == t.name) {
                    onTag(if (tagFilter == t.name) null else t.name)
                }
            }
        }

        SectionTitle("Duration")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagChip("All time", null, noDateFilter) { onAllTime() }
            DURATIONS.forEach { d ->
                TagChip("$d days", null, durationDays == d) { onDuration(d) }
            }
        }

        SectionTitle("Date range")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { pickDate(context, rangeFrom ?: System.currentTimeMillis(), onFrom) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (rangeFrom != null) "From ${shortDateFormat.format(Date(rangeFrom))}" else "From date",
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = { pickDate(context, rangeTo ?: System.currentTimeMillis(), onTo) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (rangeTo != null) "To ${shortDateFormat.format(Date(rangeTo))}" else "To date",
                    maxLines = 1
                )
            }
        }
        Text(
            "Choosing a duration clears the date range, and the other way round.",
            style = MaterialTheme.typography.bodySmall,
            color = mutedColor()
        )

        TextButton(onClick = onClearAll) {
            Text("Clear all filters", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🗒️", style = MaterialTheme.typography.headlineLarge)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = mutedColor(),
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}

@Composable
fun ViewScreen(db: HealthDb, onBack: () -> Unit, onEdit: (LogEntry) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val thresholds = remember { ThresholdStore.load(context) }
    val tagColors = remember { TagStore.colorMap(context) }

    var entries by remember { mutableStateOf(db.getAll()) } // newest -> oldest

    // Selection
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var durationDays by remember { mutableStateOf<Int?>(null) }
    var rangeFrom by remember { mutableStateOf<Long?>(null) }
    var rangeTo by remember { mutableStateOf<Long?>(null) }

    // Tags to offer in the filter: known tags + any tag still used by records
    val filterTags = remember(entries) {
        val known = TagStore.all(context)
        val extra = entries.mapNotNull { it.tag }.distinct()
            .filter { n -> known.none { it.name == n } }
            .map { Tag(it, TagPalette.FALLBACK) }
        known + extra
    }

    val filtered = remember(entries, tagFilter, durationDays, rangeFrom, rangeTo) {
        val dd = durationDays
        val from: Long? =
            if (dd != null) startOfDay(System.currentTimeMillis(), dd - 1) else rangeFrom
        val to: Long? =
            if (dd != null) null else rangeTo?.let { endOfDay(it) }
        entries.filter { e ->
            (tagFilter == null || e.tag == tagFilter) &&
                    (from == null || e.timestamp >= from) &&
                    (to == null || e.timestamp <= to)
        }
    }

    val days = remember(filtered) {
        filtered.groupBy { dayKeyFormat.format(Date(it.timestamp)) }.toList()
    }

    val dateActive = durationDays != null || rangeFrom != null || rangeTo != null
    val activeCount = (if (tagFilter != null) 1 else 0) + (if (dateActive) 1 else 0)

    val summary = buildList {
        tagFilter?.let { add("Tag: $it") }
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

    fun exitSelection() {
        selecting = false
        selectedIds = emptySet()
    }

    fun toggle(e: LogEntry) {
        selectedIds = if (e.id in selectedIds) selectedIds - e.id else selectedIds + e.id
    }

    fun clearAllFilters() {
        tagFilter = null
        durationDays = null
        rangeFrom = null
        rangeTo = null
    }

    BackHandler(enabled = selecting) { exitSelection() }

    val allSelected = filtered.isNotEmpty() && filtered.all { it.id in selectedIds }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (selecting) {
            // ----- Selection top bar -----
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { exitSelection() }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "${selectedIds.size} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        selectedIds = if (allSelected) emptySet() else filtered.map { it.id }.toSet()
                    }
                ) {
                    Text(if (allSelected) "Deselect all" else "Select all", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // ----- Normal top bar -----
            ScreenHeader("History", onBack)

            if (entries.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = "Filters" +
                            (if (activeCount > 0) " ($activeCount)" else "") +
                            (if (showFilters) "  ▴" else "  ▾")
                    TagChip(label, null, activeCount > 0 || showFilters) { showFilters = !showFilters }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (activeCount > 0) "${filtered.size} of ${entries.size} records"
                        else "${entries.size} records",
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor()
                    )
                }
                if (activeCount > 0 && !showFilters) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (!showFilters) {
                    Text(
                        "Hold a record to select it for edit or delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor(),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            EmptyState("No records yet", "Add your first log from the home screen.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (showFilters && !selecting) {
                    item(key = "filters") {
                        FilterPanel(
                            tags = filterTags,
                            tagFilter = tagFilter,
                            onTag = { tagFilter = it },
                            durationDays = durationDays,
                            onDuration = { d ->
                                durationDays = d
                                rangeFrom = null
                                rangeTo = null
                            },
                            rangeFrom = rangeFrom,
                            rangeTo = rangeTo,
                            onFrom = { d ->
                                rangeFrom = d
                                durationDays = null
                                val t = rangeTo
                                if (t != null && t < d) rangeTo = d
                            },
                            onTo = { d ->
                                rangeTo = d
                                durationDays = null
                                val f = rangeFrom
                                if (f != null && f > d) rangeFrom = d
                            },
                            onAllTime = {
                                durationDays = null
                                rangeFrom = null
                                rangeTo = null
                            },
                            onClearAll = { clearAllFilters() }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            "No records match",
                            "Try a different tag or date range.",
                            action = {
                                SecondaryButton("Clear filters", onClick = { clearAllFilters() })
                            }
                        )
                    }
                }

                items(days, key = { it.first }) { (_, dayEntries) ->
                    DayCard(
                        dayEntries = dayEntries,
                        thresholds = thresholds,
                        tagColors = tagColors,
                        selecting = selecting,
                        selectedIds = selectedIds,
                        onClick = { e -> if (selecting) toggle(e) },
                        onLongClick = { e ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!selecting) {
                                selecting = true
                                selectedIds = setOf(e.id)
                            } else {
                                toggle(e)
                            }
                        }
                    )
                }
            }
        }

        // ----- Bottom action bar (only while selecting) -----
        if (selecting) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Edit",
                    onClick = {
                        val one = entries.firstOrNull { it.id in selectedIds }
                        if (one != null) onEdit(one)
                    },
                    enabled = selectedIds.size == 1,
                    modifier = Modifier.weight(1f)
                )
                DangerButton(
                    text = if (selectedIds.isEmpty()) "Delete" else "Delete (${selectedIds.size})",
                    onClick = { confirmDelete = true },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (confirmDelete) {
        val n = selectedIds.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (n == 1) "Delete 1 record?" else "Delete $n records?") },
            text = { Text("They will be removed permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedIds.forEach { db.delete(it) }
                        entries = db.getAll()
                        confirmDelete = false
                        exitSelection()
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}