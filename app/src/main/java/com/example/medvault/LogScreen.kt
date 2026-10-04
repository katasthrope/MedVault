package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
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
    val heightCm = remember { ProfileStore.load(context).heightCm }
    val thresholds = remember { ThresholdStore.load(context) }

    var time by remember { mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var glucose by remember { mutableStateOf(existing?.glucose?.let { fmt(it) } ?: "") }
    var uric by remember { mutableStateOf(existing?.uricAcid?.let { fmt(it) } ?: "") }
    var sys by remember { mutableStateOf(existing?.systolic?.toString() ?: "") }
    var dia by remember { mutableStateOf(existing?.diastolic?.toString() ?: "") }
    var weight by remember { mutableStateOf(existing?.weight?.let { fmt(it) } ?: "") }
    var remark by remember { mutableStateOf(existing?.remark ?: "") }
    var tag by remember { mutableStateOf(existing?.tag) }

    val tagOptions = remember {
        val base = TagStore.all(context)
        val own = existing?.tag
        if (own != null && base.none { it.name == own }) base + Tag(own, TagPalette.FALLBACK) else base
    }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val liveWeight = weight.trim().replace(',', '.').toDoubleOrNull()
    val liveBmi = bmiOf(liveWeight, heightCm)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader(if (isEdit) "Edit log" else "New log", onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                SectionTitle("When")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { pickDateTime(context, time) { time = it } }) {
                        Text("Change", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SectionCard {
                SectionTitle("Readings")
                OutlinedTextField(
                    value = glucose, onValueChange = { glucose = it },
                    label = { Text("Glucose") }, suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uric, onValueChange = { uric = it },
                    label = { Text("Uric acid") }, suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                Text("Blood pressure (mmHg)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sys, onValueChange = { sys = it },
                        label = { Text("Systolic") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, shape = FieldShape, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dia, onValueChange = { dia = it },
                        label = { Text("Diastolic") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, shape = FieldShape, modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "Fill both systolic and diastolic, or leave both empty.",
                    style = MaterialTheme.typography.bodySmall, color = muted
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("Weight") }, suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                if (weight.isNotBlank()) {
                    if (liveBmi != null) {
                        val out = thresholds.bmi.isOut(liveBmi)
                        Text(
                            "BMI ${Metric.BMI.display(liveBmi)} kg/m²",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (out) MaterialTheme.colorScheme.error else muted
                        )
                    } else if (heightCm == null) {
                        Text(
                            "Set your height in Profile to see BMI.",
                            style = MaterialTheme.typography.bodySmall, color = muted
                        )
                    }
                }
            }

            SectionCard {
                SectionTitle("Tag")
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
                Text("Optional. Tap again to clear.", style = MaterialTheme.typography.bodySmall, color = muted)
            }

            SectionCard {
                SectionTitle("Remark")
                OutlinedTextField(
                    value = remark, onValueChange = { remark = it },
                    label = { Text("Remark") }, minLines = 3,
                    shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PrimaryButton(
            text = if (isEdit) "Update" else "Save",
            onClick = {
                val gText = glucose.trim().replace(',', '.')
                val uText = uric.trim().replace(',', '.')
                val wText = weight.trim().replace(',', '.')
                val g = gText.toDoubleOrNull()
                val u = uText.toDoubleOrNull()
                val w = wText.toDoubleOrNull()
                val s = sys.trim().toIntOrNull()
                val d = dia.trim().toIntOrNull()
                val sysEmpty = sys.isBlank()
                val diaEmpty = dia.isBlank()

                when {
                    gText.isNotEmpty() && g == null ->
                        showMessage("Glucose is not a valid number")
                    uText.isNotEmpty() && u == null ->
                        showMessage("Uric acid is not a valid number")
                    wText.isNotEmpty() && (w == null || w <= 0.0) ->
                        showMessage("Weight is not a valid number")
                    sysEmpty != diaEmpty ->
                        showMessage("Fill both systolic and diastolic, or leave both empty")
                    g == null && u == null && w == null && sysEmpty && diaEmpty ->
                        showMessage("Fill at least one: glucose, uric acid, blood pressure or weight")
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
                            remark = remark.trim(),
                            tag = tag,
                            weight = w
                        )
                        if (isEdit) {
                            db.update(entry)
                            showMessage("Updated")
                        } else {
                            db.insert(entry)
                            showMessage("Saved")
                            glucose = ""; uric = ""; sys = ""; dia = ""; weight = ""; remark = ""
                            tag = null
                            time = System.currentTimeMillis()
                        }
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}