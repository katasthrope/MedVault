package com.example.medvault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val docDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
private fun DocCard(
    d: Document,
    tagColors: Map<String, Int>,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(d.file) { d.file?.let { FileStore.file(context, "docs", it) } }

    SectionCard {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = file != null, onClick = onOpen),
            verticalAlignment = Alignment.Top
        ) {
            Thumb(file, d.mime, 84.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    d.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                if (d.tag != null) TagPill(d.tag, tagColors[d.tag] ?: TagPalette.FALLBACK)
                Text(
                    docDateFormat.format(Date(d.dateMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor()
                )
                if (d.note.isNotBlank()) {
                    Text(d.note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                if (file == null) {
                    Text("No file attached", style = MaterialTheme.typography.labelSmall, color = mutedColor())
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (file != null) {
                TextButton(onClick = onOpen) { Text("View", fontWeight = FontWeight.SemiBold) }
            }
            TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentEditor(
    db: HealthDb,
    existing: Document?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var tag by remember { mutableStateOf(existing?.tag) }
    var dateMs by remember { mutableLongStateOf(existing?.dateMs ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var pending by remember { mutableStateOf<FileStore.Imported?>(null) }

    val tagOptions = remember {
        val base = DocTagStore.all(context)
        val own = existing?.tag
        if (own != null && base.none { it.name == own }) base + Tag(own, TagPalette.FALLBACK) else base
    }

    val pick = rememberFilePicker("docs") { imp ->
        if (imp != null) {
            pending?.let { FileStore.delete(context, "docs", it.storedName) }
            pending = imp
            if (name.isBlank()) name = imp.displayName.substringBeforeLast('.')
        } else {
            showMessage("Could not read that file")
        }
    }

    fun cancel() {
        pending?.let { FileStore.delete(context, "docs", it.storedName) }
        onClose()
    }
    BackHandler { cancel() }

    val shownFile = pending?.let { FileStore.file(context, "docs", it.storedName) }
        ?: existing?.file?.let { FileStore.file(context, "docs", it) }
    val shownMime = pending?.mime ?: existing?.mime

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader(if (existing == null) "Add document" else "Edit document", onBack = { cancel() })

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                SectionTitle("File")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumb(shownFile, shownMime, 84.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            pending?.displayName ?: if (shownFile != null) "Current file" else "No file chosen",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        SecondaryButton(
                            text = if (shownFile == null) "Choose file" else "Replace file",
                            onClick = pick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text("Photos and PDFs are supported.", style = MaterialTheme.typography.bodySmall, color = mutedColor())
            }

            SectionCard {
                SectionTitle("Details")
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                Text("Tag", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagOptions.forEach { option ->
                        TagChip(option.name, option.colorIndex, tag == option.name) {
                            tag = if (tag == option.name) null else option.name
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Date", style = MaterialTheme.typography.labelLarge)
                        Text(
                            docDateFormat.format(Date(dateMs)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { pickDate(context, dateMs) { dateMs = it } }) {
                        Text("Change", fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    minLines = 2, shape = FieldShape, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default
                )
            }
        }

        PrimaryButton(
            text = if (existing == null) "Save document" else "Update document",
            onClick = {
                val fileName = pending?.storedName ?: existing?.file
                when {
                    name.isBlank() -> showMessage("Please give the document a name")
                    existing == null && fileName == null -> showMessage("Please choose a file")
                    else -> {
                        val doc = Document(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            tag = tag,
                            dateMs = dateMs,
                            note = note.trim(),
                            file = fileName,
                            mime = pending?.mime ?: existing?.mime
                        )
                        if (existing == null) {
                            db.insertDocument(doc)
                            showMessage("Document saved")
                        } else {
                            db.updateDocument(doc)
                            if (pending != null) FileStore.delete(context, "docs", existing.file)
                            showMessage("Document updated")
                        }
                        pending = null
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DocumentsScreen(db: HealthDb, onBack: () -> Unit, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    var docs by remember { mutableStateOf(db.getDocuments()) }
    val tagColors = remember { DocTagStore.colorMap(context) }
    var filterTag by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Document?>(null) }
    var toDelete by remember { mutableStateOf<Document?>(null) }
    var viewing by remember { mutableStateOf<Document?>(null) }

    if (editorOpen) {
        DocumentEditor(
            db = db,
            existing = editing,
            onClose = { editorOpen = false },
            onSaved = {
                docs = db.getDocuments()
                editorOpen = false
            },
            showMessage = showMessage
        )
        return
    }

    val tagsInUse = remember(docs) {
        val known = DocTagStore.all(context)
        val extra = docs.mapNotNull { it.tag }.distinct()
            .filter { n -> known.none { it.name == n } }
            .map { Tag(it, TagPalette.FALLBACK) }
        known + extra
    }
    val shown = remember(docs, filterTag) { docs.filter { filterTag == null || it.tag == filterTag } }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Medical records", onBack)

        if (docs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    "No documents yet",
                    "Upload lab results, scans, doctor's notes, invoices and more."
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagChip("All", null, filterTag == null) { filterTag = null }
                tagsInUse.forEach { t ->
                    TagChip(t.name, t.colorIndex, filterTag == t.name) {
                        filterTag = if (filterTag == t.name) null else t.name
                    }
                }
            }
            Text(
                if (filterTag == null) "${docs.size} documents" else "${shown.size} of ${docs.size} documents",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor(),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (shown.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState("Nothing with that tag", "Choose another tag or tap All.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(shown, key = { it.id }) { d ->
                        DocCard(
                            d = d,
                            tagColors = tagColors,
                            onOpen = { viewing = d },
                            onEdit = { editing = d; editorOpen = true },
                            onDelete = { toDelete = d }
                        )
                    }
                }
            }
        }

        PrimaryButton(
            text = "+ Add document",
            onClick = { editing = null; editorOpen = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
        )
    }

    viewing?.let { d ->
        val f = d.file
        if (f != null) {
            FileViewerDialog(d.name, FileStore.file(context, "docs", f), d.mime) { viewing = null }
        }
    }

    toDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${d.name}\"?") },
            text = { Text("The document and its file will be removed from this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        FileStore.delete(context, "docs", d.file)
                        db.deleteDocument(d.id)
                        docs = db.getDocuments()
                        toDelete = null
                        showMessage("Document deleted")
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}