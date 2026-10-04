package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FREQUENCIES = listOf(
    "Once daily", "Twice daily", "3 times daily", "Every other day", "Weekly", "As needed"
)

@Composable
private fun InfoBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = mutedColor())
        Text(
            value.ifBlank { "–" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MedCard(m: Medication, onEdit: () -> Unit, onDelete: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text("💊", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Text(
                m.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBlock("Dosage", m.dosage, Modifier.weight(1f))
            InfoBlock("Frequency", m.frequency, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedDialog(
    initial: Medication?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    showMessage: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add medication" else "Edit medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Medication name") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage, onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 500 mg)") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = frequency, onValueChange = { frequency = it },
                    label = { Text("Frequency") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FREQUENCIES.forEach { f ->
                        TagChip(f, null, frequency == f) { frequency = f }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || dosage.isBlank() || frequency.isBlank()) {
                        showMessage("Please fill in name, dosage and frequency")
                    } else {
                        onSave(name.trim(), dosage.trim(), frequency.trim())
                    }
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MedicationScreen(db: HealthDb, onBack: () -> Unit, showMessage: (String) -> Unit) {
    var meds by remember { mutableStateOf(db.getMedications()) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medication?>(null) }
    var toDelete by remember { mutableStateOf<Medication?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Medication", onBack)

        if (meds.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState("No medication yet", "Add what you are currently taking.")
            }
        } else {
            Text(
                "Currently taking · ${meds.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor(),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(meds, key = { it.id }) { m ->
                    MedCard(
                        m = m,
                        onEdit = { editing = m; dialogOpen = true },
                        onDelete = { toDelete = m }
                    )
                }
            }
        }

        PrimaryButton(
            text = "+ Add medication",
            onClick = { editing = null; dialogOpen = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
        )
    }

    if (dialogOpen) {
        MedDialog(
            initial = editing,
            onDismiss = { dialogOpen = false },
            onSave = { name, dosage, frequency ->
                val current = editing
                if (current == null) {
                    db.insertMedication(Medication(name = name, dosage = dosage, frequency = frequency))
                    showMessage("Medication added")
                } else {
                    db.updateMedication(current.copy(name = name, dosage = dosage, frequency = frequency))
                    showMessage("Medication updated")
                }
                meds = db.getMedications()
                dialogOpen = false
            },
            showMessage = showMessage
        )
    }

    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${m.name}?") },
            text = { Text("It will be removed from your medication list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.deleteMedication(m.id)
                        meds = db.getMedications()
                        toDelete = null
                        showMessage("Medication deleted")
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}