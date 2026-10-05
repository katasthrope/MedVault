package com.example.medvault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

@Composable
private fun InsuranceCard(
    ins: Insurance,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(ins.ecard) { ins.ecard?.let { FileStore.file(context, "ecards", it) } }

    SectionCard {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = file != null, onClick = onOpen),
            verticalAlignment = Alignment.Top
        ) {
            Thumb(file, ins.ecardMime, 84.dp, emptyIcon = "💳")
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    ins.provider,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text("Policy number", style = MaterialTheme.typography.labelSmall, color = mutedColor())
                Text(
                    ins.policyNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (ins.notes.isNotBlank()) {
                    Text(ins.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = mutedColor())
                }
                if (file == null) {
                    Text("No e-card uploaded", style = MaterialTheme.typography.labelSmall, color = mutedColor())
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCopy) { Text("Copy number", fontWeight = FontWeight.SemiBold) }
            if (file != null) {
                TextButton(onClick = onOpen) { Text("E-card", fontWeight = FontWeight.SemiBold) }
            }
            TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InsuranceEditor(
    db: HealthDb,
    existing: Insurance?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var provider by remember { mutableStateOf(existing?.provider ?: "") }
    var policy by remember { mutableStateOf(existing?.policyNumber ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var pending by remember { mutableStateOf<FileStore.Imported?>(null) }

    val pick = rememberFilePicker("ecards") { imp ->
        if (imp != null) {
            pending?.let { FileStore.delete(context, "ecards", it.storedName) }
            pending = imp
        } else {
            showMessage("Could not read that file")
        }
    }

    fun cancel() {
        pending?.let { FileStore.delete(context, "ecards", it.storedName) }
        onClose()
    }
    BackHandler { cancel() }

    val shownFile = pending?.let { FileStore.file(context, "ecards", it.storedName) }
        ?: existing?.ecard?.let { FileStore.file(context, "ecards", it) }
    val shownMime = pending?.mime ?: existing?.ecardMime

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader(if (existing == null) "Add insurance" else "Edit insurance", onBack = { cancel() })

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                SectionTitle("Details")
                OutlinedTextField(
                    value = provider, onValueChange = { provider = it },
                    label = { Text("Provider") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = policy, onValueChange = { policy = it },
                    label = { Text("Policy number") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (plan, member ID, valid until…)") },
                    minLines = 2, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard {
                SectionTitle("E-card")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumb(shownFile, shownMime, 84.dp, emptyIcon = "💳")
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            pending?.displayName ?: if (shownFile != null) "Current e-card" else "No e-card chosen",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        SecondaryButton(
                            text = if (shownFile == null) "Upload e-card" else "Replace e-card",
                            onClick = pick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text("A photo or PDF of your insurance card.", style = MaterialTheme.typography.bodySmall, color = mutedColor())
            }
        }

        PrimaryButton(
            text = if (existing == null) "Save insurance" else "Update insurance",
            onClick = {
                when {
                    provider.isBlank() -> showMessage("Please enter the provider")
                    policy.isBlank() -> showMessage("Please enter the policy number")
                    else -> {
                        val ins = Insurance(
                            id = existing?.id ?: 0,
                            provider = provider.trim(),
                            policyNumber = policy.trim(),
                            notes = notes.trim(),
                            ecard = pending?.storedName ?: existing?.ecard,
                            ecardMime = pending?.mime ?: existing?.ecardMime
                        )
                        if (existing == null) {
                            db.insertInsurance(ins)
                            showMessage("Insurance saved")
                        } else {
                            db.updateInsurance(ins)
                            if (pending != null) FileStore.delete(context, "ecards", existing.ecard)
                            showMessage("Insurance updated")
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
fun InsuranceScreen(db: HealthDb, onBack: () -> Unit, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(db.getInsurances()) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Insurance?>(null) }
    var toDelete by remember { mutableStateOf<Insurance?>(null) }
    var viewing by remember { mutableStateOf<Insurance?>(null) }

    if (editorOpen) {
        InsuranceEditor(
            db = db,
            existing = editing,
            onClose = { editorOpen = false },
            onSaved = {
                list = db.getInsurances()
                editorOpen = false
            },
            showMessage = showMessage
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Insurance", onBack)

        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState("No insurance yet", "Add each policy with its provider, number and e-card.")
            }
        } else {
            Text(
                "${list.size} ${if (list.size == 1) "policy" else "policies"}",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor(),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(list, key = { it.id }) { ins ->
                    InsuranceCard(
                        ins = ins,
                        onOpen = { viewing = ins },
                        onEdit = { editing = ins; editorOpen = true },
                        onDelete = { toDelete = ins },
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Policy number", ins.policyNumber))
                            showMessage("Policy number copied")
                        }
                    )
                }
            }
        }

        PrimaryButton(
            text = "+ Add insurance",
            onClick = { editing = null; editorOpen = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
        )
    }

    viewing?.let { ins ->
        val f = ins.ecard
        if (f != null) {
            FileViewerDialog(ins.provider, FileStore.file(context, "ecards", f), ins.ecardMime) { viewing = null }
        }
    }

    toDelete?.let { ins ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${ins.provider}?") },
            text = { Text("The policy details and e-card will be removed from this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        FileStore.delete(context, "ecards", ins.ecard)
                        db.deleteInsurance(ins.id)
                        list = db.getInsurances()
                        toDelete = null
                        showMessage("Insurance deleted")
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}