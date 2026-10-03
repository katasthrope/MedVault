package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFormat =
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

private val clockFormat =
    SimpleDateFormat("HH:mm", Locale.getDefault())


@Composable
private fun flagged(out: Boolean): SpanStyle =
    if (out) {
        SpanStyle(
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    } else {
        SpanStyle()
    }


@Composable
private fun valueText(
    text: String,
    out: Boolean
): AnnotatedString {

    val style = flagged(out)

    return buildAnnotatedString {
        withStyle(style) {
            append(text)
        }
    }
}


@Composable
private fun Metric(
    label: String,
    unit: String,
    value: AnnotatedString?,
    modifier: Modifier = Modifier
) {
    val muted =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Column(modifier) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted
        )

        if (value != null) {

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )

        } else {

            Text(
                text = "–",
                style = MaterialTheme.typography.titleMedium,
                color = muted
            )

            Text(
                text = " ",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


/**
 * One measurement inside a day's card.
 */
@Composable
private fun DayEntry(
    e: LogEntry,
    thresholds: Thresholds,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val muted =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    val glucoseText =
        e.glucose?.let {
            valueText(
                fmt(it),
                thresholds.glucose.isOut(it)
            )
        }

    val uricText =
        e.uricAcid?.let {
            valueText(
                fmt(it),
                thresholds.uric.isOut(it)
            )
        }

    val sysStyle =
        flagged(
            e.systolic != null &&
                    thresholds.systolic.isOut(
                        e.systolic.toDouble()
                    )
        )

    val diaStyle =
        flagged(
            e.diastolic != null &&
                    thresholds.diastolic.isOut(
                        e.diastolic.toDouble()
                    )
        )

    val bpText: AnnotatedString? =
        if (e.systolic != null && e.diastolic != null) {

            buildAnnotatedString {

                withStyle(sysStyle) {
                    append("${e.systolic}")
                }

                append("/")

                withStyle(diaStyle) {
                    append("${e.diastolic}")
                }
            }

        } else {
            null
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        /*
         * Time + checkbox
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = clockFormat.format(Date(e.timestamp)),
                style = MaterialTheme.typography.labelLarge,
                color = muted,
                modifier = Modifier.weight(1f)
            )

            if (selectionMode) {

                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectionChanged
                )
            }
        }


        /*
         * Readings
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Metric(
                label = "Glucose",
                unit = "mg/dL",
                value = glucoseText,
                modifier = Modifier.weight(1f)
            )

            Metric(
                label = "Uric acid",
                unit = "mg/dL",
                value = uricText,
                modifier = Modifier.weight(1f)
            )

            Metric(
                label = "Blood pressure",
                unit = "mmHg",
                value = bpText,
                modifier = Modifier.weight(1.2f)
            )
        }


        /*
         * Remark
         */
        if (e.remark.isNotBlank()) {

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = "Remark",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )

                Text(
                    text = e.remark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }


        /*
         * Normal Edit/Delete buttons.
         *
         * Hide these while selection mode is active.
         */
        if (!selectionMode) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = onEdit
                ) {
                    Text("Edit")
                }

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}


/**
 * One card = one calendar day.
 */
@Composable
private fun DayCard(
    date: Long,
    entries: List<LogEntry>,
    thresholds: Thresholds,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onSelectionChanged: (Long, Boolean) -> Unit,
    onEdit: (LogEntry) -> Unit,
    onDelete: (LogEntry) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            /*
             * Day header
             */
            Text(
                text = dayFormat.format(Date(date)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )


            /*
             * Entries belonging to this day
             */
            entries.forEachIndexed { index, entry ->

                if (index > 0) {

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = 4.dp
                        )
                    )
                }

                DayEntry(
                    e = entry,
                    thresholds = thresholds,
                    selectionMode = selectionMode,
                    selected = selectedIds.contains(entry.id),
                    onSelectionChanged = {
                        onSelectionChanged(
                            entry.id,
                            it
                        )
                    },
                    onEdit = {
                        onEdit(entry)
                    },
                    onDelete = {
                        onDelete(entry)
                    }
                )
            }
        }
    }
}


@Composable
fun ViewScreen(
    db: HealthDb,
    onBack: () -> Unit,
    onEdit: (LogEntry) -> Unit
) {

    val context = LocalContext.current

    val thresholds = remember {
        ThresholdStore.load(context)
    }


    /*
     * All database entries.
     */
    var entries by remember {
        mutableStateOf(db.getAll())
    }


    /*
     * Selection mode.
     */
    var selectionMode by remember {
        mutableStateOf(false)
    }


    /*
     * IDs of selected records.
     */
    var selectedIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }


    /*
     * Delete confirmation.
     */
    var showDeleteSelectedDialog by remember {
        mutableStateOf(false)
    }


    var toDelete by remember {
        mutableStateOf<LogEntry?>(null)
    }


    /*
     * Group records by day.
     */
    val groupedEntries = remember(entries) {

        entries.groupBy { entry ->
            dayFormat.format(Date(entry.timestamp))
        }
    }


    /*
     * Enter / exit selection mode.
     */
    fun exitSelectionMode() {

        selectionMode = false
        selectedIds = emptySet()
    }


    /*
     * Select all records.
     */
    fun selectAll() {

        selectedIds = entries
            .map { it.id }
            .toSet()
    }


    /*
     * Deselect all.
     */
    fun deselectAll() {

        selectedIds = emptySet()
    }


    /*
     * Delete selected records.
     */
    fun deleteSelected() {

        selectedIds.forEach { id ->
            db.delete(id)
        }

        entries = db.getAll()

        selectedIds = emptySet()

        selectionMode = false

        showDeleteSelectedDialog = false
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


        /*
         * Header
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!selectionMode) {

                TextButton(
                    onClick = onBack
                ) {
                    Text("< Back")
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                /*
                 * Enter selection mode
                 */
                TextButton(
                    onClick = {
                        selectionMode = true
                        selectedIds = emptySet()
                    }
                ) {
                    Text("Select")
                }

            } else {

                /*
                 * Selection mode header
                 */
                TextButton(
                    onClick = {
                        exitSelectionMode()
                    }
                ) {
                    Text("Cancel")
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "${selectedIds.size} selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                /*
                 * Select all / deselect all
                 */
                TextButton(
                    onClick = {

                        if (selectedIds.size == entries.size) {
                            deselectAll()
                        } else {
                            selectAll()
                        }
                    }
                ) {

                    Text(
                        if (selectedIds.size == entries.size) {
                            "Deselect all"
                        } else {
                            "Select all"
                        }
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        /*
         * Empty state
         */
        if (entries.isEmpty()) {

            Text("No logs yet.")

        } else {

            /*
             * History list
             */
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    bottom = 16.dp
                )
            ) {

                groupedEntries.forEach { (_, dayEntries) ->

                    item(
                        key = "day_${dayEntries.first().timestamp}"
                    ) {

                        DayCard(
                            date = dayEntries.first().timestamp,
                            entries = dayEntries,
                            thresholds = thresholds,
                            selectionMode = selectionMode,
                            selectedIds = selectedIds,

                            onSelectionChanged = { id, selected ->

                                selectedIds =
                                    if (selected) {
                                        selectedIds + id
                                    } else {
                                        selectedIds - id
                                    }
                            },

                            onEdit = onEdit,

                            onDelete = {
                                toDelete = it
                            }
                        )
                    }
                }
            }
        }


        /*
         * Delete selected button.
         *
         * Only visible when at least one record
         * is selected.
         */
        if (selectionMode && selectedIds.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = {
                    showDeleteSelectedDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor =
                        MaterialTheme.colorScheme.error
                )
            ) {

                Text(
                    "Delete ${selectedIds.size} selected"
                )
            }
        }
    }


    /*
     * Delete selected confirmation.
     */
    if (showDeleteSelectedDialog) {

        AlertDialog(

            onDismissRequest = {
                showDeleteSelectedDialog = false
            },

            title = {
                Text("Delete selected records?")
            },

            text = {
                Text(
                    "${selectedIds.size} record(s) will be " +
                            "removed permanently."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        deleteSelected()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteSelectedDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    /*
     * Delete one record confirmation.
     */
    toDelete?.let { entry ->

        AlertDialog(

            onDismissRequest = {
                toDelete = null
            },

            title = {
                Text("Delete entry?")
            },

            text = {
                Text(
                    "The log from ${formatTime(entry.timestamp)} " +
                            "will be removed permanently."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        db.delete(entry.id)

                        entries = db.getAll()

                        toDelete = null
                    },

                    colors = ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        toDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}