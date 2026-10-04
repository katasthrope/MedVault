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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
private val weekdayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
private val dayNumFormat = SimpleDateFormat("dd", Locale.getDefault())
private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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
    heightCm: Double?,
    tagColors: Map<String, Int>,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val muted = mutedColor()
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
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clockFormat.format(Date(e.timestamp)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (selecting) SelectDot(selected)
            }

            ReadingTiles(e, thresholds, heightCm)

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
    heightCm: Double?,
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
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(1.dp).background(line))
                    Spacer(Modifier.height(6.dp))
                }
                EntryBlock(
                    e = e,
                    thresholds = thresholds,
                    heightCm = heightCm,
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

@Composable
fun ViewScreen(db: HealthDb, onBack: () -> Unit, onEdit: (LogEntry) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val thresholds = remember { ThresholdStore.load(context) }
    val heightCm = remember { ProfileStore.load(context).heightCm }
    val tagColors = remember { TagStore.colorMap(context) }

    var entries by remember { mutableStateOf(db.getAll()) } // newest -> oldest

    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    var showFilters by remember { mutableStateOf(false) }
    val f = remember { FilterState() }
    val filterTags = remember(entries) { tagsForFilter(context, entries) }
    val filtered = remember(entries, f.tag, f.durationDays, f.rangeFrom, f.rangeTo) { f.apply(entries) }
    val days = remember(filtered) {
        filtered.groupBy { dayKeyFormat.format(Date(it.timestamp)) }.toList()
    }

    fun exitSelection() {
        selecting = false
        selectedIds = emptySet()
    }

    fun toggle(e: LogEntry) {
        selectedIds = if (e.id in selectedIds) selectedIds - e.id else selectedIds + e.id
    }

    BackHandler(enabled = selecting) { exitSelection() }

    val allSelected = filtered.isNotEmpty() && filtered.all { it.id in selectedIds }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (selecting) {
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
            ScreenHeader("History", onBack)
            if (entries.isNotEmpty()) {
                FilterHeader(
                    state = f,
                    expanded = showFilters,
                    onToggle = { showFilters = !showFilters },
                    shown = filtered.size,
                    total = entries.size,
                    hint = "Hold a record to select it for edit or delete"
                )
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
                }

                items(days, key = { it.first }) { (_, dayEntries) ->
                    DayCard(
                        dayEntries = dayEntries,
                        thresholds = thresholds,
                        heightCm = heightCm,
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
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}