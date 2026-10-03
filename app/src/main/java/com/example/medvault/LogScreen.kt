package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LogScreen(
    db: HealthDb,
    existing: LogEntry?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val isEdit = existing != null

    var time by remember { mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var glucose by remember { mutableStateOf(existing?.glucose?.let { fmt(it) } ?: "") }
    var uric by remember { mutableStateOf(existing?.uricAcid?.let { fmt(it) } ?: "") }
    var sys by remember { mutableStateOf(existing?.systolic?.toString() ?: "") }
    var dia by remember { mutableStateOf(existing?.diastolic?.toString() ?: "") }
    var remark by remember { mutableStateOf(existing?.remark ?: "") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.width(8.dp))
            Text(if (isEdit) "Edit Log" else "New Log", style = MaterialTheme.typography.titleLarge)
        }

        Text("Date & time", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(time), modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { pickDateTime(context, time) { time = it } }) {
                Text("Change")
            }
        }

        OutlinedTextField(
            value = glucose,
            onValueChange = { glucose = it },
            label = { Text("Glucose (mg/dL)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uric,
            onValueChange = { uric = it },
            label = { Text("Uric acid (mg/dL)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sys,
                onValueChange = { sys = it },
                label = { Text("Systolic") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = dia,
                onValueChange = { dia = it },
                label = { Text("Diastolic") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Text("Blood pressure in mmHg", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = remark,
            onValueChange = { remark = it },
            label = { Text("Remark") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val gText = glucose.trim().replace(',', '.')
                val uText = uric.trim().replace(',', '.')
                val g = gText.toDoubleOrNull()
                val u = uText.toDoubleOrNull()
                val s = sys.trim().toIntOrNull()
                val d = dia.trim().toIntOrNull()
                val sysEmpty = sys.isBlank()
                val diaEmpty = dia.isBlank()

                when {
                    gText.isNotEmpty() && g == null ->
                        showMessage("Glucose is not a valid number")
                    uText.isNotEmpty() && u == null ->
                        showMessage("Uric acid is not a valid number")
                    sysEmpty != diaEmpty ->
                        showMessage("Fill both systolic and diastolic, or leave both empty")
                    g == null && u == null && sysEmpty && diaEmpty ->
                        showMessage("Fill at least one: glucose, uric acid, or blood pressure")
                    !sysEmpty && (s == null || d == null) ->
                        showMessage("Blood pressure must be whole numbers")
                    else -> {
                        val entry = LogEntry(
                            id = existing?.id ?: 0,
                            timestamp = time,
                            glucose = g,
                            uricAcid = u,
                            systolic = s,
                            diastolic = d,
                            remark = remark.trim()
                        )
                        if (isEdit) {
                            db.update(entry)
                            showMessage("Updated")
                        } else {
                            db.insert(entry)
                            showMessage("Saved")
                            glucose = ""; uric = ""; sys = ""; dia = ""; remark = ""
                            time = System.currentTimeMillis()
                        }
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (isEdit) "Update" else "Save")
        }
    }
}