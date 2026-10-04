package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private fun parseNumber(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()

private fun rangeText(v: Double?): String = v?.let { fmt(it) } ?: ""

private fun problem(name: String, lowText: String, highText: String): String? {
    if (lowText.isNotBlank() && parseNumber(lowText) == null) return "$name: low is not a valid number"
    if (highText.isNotBlank() && parseNumber(highText) == null) return "$name: high is not a valid number"
    val l = parseNumber(lowText)
    val h = parseNumber(highText)
    if (l != null && h != null && l > h) return "$name: low must not be above high"
    return null
}

@Composable
private fun RangeRow(
    title: String,
    low: String,
    onLow: (String) -> Unit,
    high: String,
    onHigh: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = low, onValueChange = onLow, label = { Text("Low") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, shape = FieldShape, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = high, onValueChange = onHigh, label = { Text("High") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, shape = FieldShape, modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    db: HealthDb,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { ThresholdStore.load(context) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var gluLow by remember { mutableStateOf(rangeText(saved.glucose.low)) }
    var gluHigh by remember { mutableStateOf(rangeText(saved.glucose.high)) }
    var uricLow by remember { mutableStateOf(rangeText(saved.uric.low)) }
    var uricHigh by remember { mutableStateOf(rangeText(saved.uric.high)) }
    var sysLow by remember { mutableStateOf(rangeText(saved.systolic.low)) }
    var sysHigh by remember { mutableStateOf(rangeText(saved.systolic.high)) }
    var diaLow by remember { mutableStateOf(rangeText(saved.diastolic.low)) }
    var diaHigh by remember { mutableStateOf(rangeText(saved.diastolic.high)) }
    var wtLow by remember { mutableStateOf(rangeText(saved.weight.low)) }
    var wtHigh by remember { mutableStateOf(rangeText(saved.weight.high)) }
    var bmiLow by remember { mutableStateOf(rangeText(saved.bmi.low)) }
    var bmiHigh by remember { mutableStateOf(rangeText(saved.bmi.high)) }

    var customTags by remember { mutableStateOf(TagStore.custom(context)) }
    var newTag by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(TagPalette.DEFAULT_CUSTOM) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var confirmSample by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Settings", onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ----- Appearance -----
            SectionCard {
                SectionTitle("Appearance")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Text("Switch between light and dark", style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    Text("☀️")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = darkTheme, onCheckedChange = onThemeChange)
                    Spacer(Modifier.width(8.dp))
                    Text("🌙")
                }
            }

            // ----- Thresholds -----
            SectionCard {
                SectionTitle("Thresholds")
                Text(
                    "Values below Low or above High appear in red. Leave a field empty for no limit. " +
                            "The starting values are general adult reference values, so change them to " +
                            "the limits your doctor gave you.",
                    style = MaterialTheme.typography.bodySmall, color = muted
                )

                RangeRow("Glucose (mg/dL)", gluLow, { gluLow = it }, gluHigh, { gluHigh = it })
                RangeRow("Uric acid (mg/dL)", uricLow, { uricLow = it }, uricHigh, { uricHigh = it })
                RangeRow("Systolic (mmHg)", sysLow, { sysLow = it }, sysHigh, { sysHigh = it })
                RangeRow("Diastolic (mmHg)", diaLow, { diaLow = it }, diaHigh, { diaHigh = it })
                RangeRow("Weight (kg)", wtLow, { wtLow = it }, wtHigh, { wtHigh = it })
                RangeRow("BMI (kg/m²)", bmiLow, { bmiLow = it }, bmiHigh, { bmiHigh = it })

                PrimaryButton(
                    text = "Save thresholds",
                    onClick = {
                        val error = problem("Glucose", gluLow, gluHigh)
                            ?: problem("Uric acid", uricLow, uricHigh)
                            ?: problem("Systolic", sysLow, sysHigh)
                            ?: problem("Diastolic", diaLow, diaHigh)
                            ?: problem("Weight", wtLow, wtHigh)
                            ?: problem("BMI", bmiLow, bmiHigh)
                        if (error != null) {
                            showMessage(error)
                        } else {
                            ThresholdStore.save(
                                context,
                                Thresholds(
                                    glucose = Range(parseNumber(gluLow), parseNumber(gluHigh)),
                                    uric = Range(parseNumber(uricLow), parseNumber(uricHigh)),
                                    systolic = Range(parseNumber(sysLow), parseNumber(sysHigh)),
                                    diastolic = Range(parseNumber(diaLow), parseNumber(diaHigh)),
                                    weight = Range(parseNumber(wtLow), parseNumber(wtHigh)),
                                    bmi = Range(parseNumber(bmiLow), parseNumber(bmiHigh))
                                )
                            )
                            showMessage("Thresholds saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(
                    text = "Reset to defaults",
                    onClick = {
                        val d = Thresholds.DEFAULT
                        gluLow = rangeText(d.glucose.low);    gluHigh = rangeText(d.glucose.high)
                        uricLow = rangeText(d.uric.low);      uricHigh = rangeText(d.uric.high)
                        sysLow = rangeText(d.systolic.low);   sysHigh = rangeText(d.systolic.high)
                        diaLow = rangeText(d.diastolic.low);  diaHigh = rangeText(d.diastolic.high)
                        wtLow = rangeText(d.weight.low);      wtHigh = rangeText(d.weight.high)
                        bmiLow = rangeText(d.bmi.low);        bmiHigh = rangeText(d.bmi.high)
                        showMessage("Defaults filled in. Tap Save thresholds to apply.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ----- Tags -----
            SectionCard {
                SectionTitle("Tags")
                Text("Built-in", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagStore.PRESETS.forEach { TagPill(it.name, it.colorIndex) }
                }

                Text("Your tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (customTags.isEmpty()) {
                    Text("No custom tags yet.", style = MaterialTheme.typography.bodySmall, color = muted)
                } else {
                    Text("Tap a color circle to change its color.", style = MaterialTheme.typography.bodySmall, color = muted)
                }
                customTags.forEach { tag ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TagPalette.get(tag.colorIndex).base)
                                .clickable { editingTag = tag }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(tag.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                TagStore.remove(context, tag.name)
                                customTags = TagStore.custom(context)
                                showMessage("Tag removed. Existing records keep it.")
                            }
                        ) {
                            Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("Add a tag", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = newTag, onValueChange = { newTag = it },
                    label = { Text("New tag name") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                ColorSwatches(selected = newColor, onSelect = { newColor = it })
                PrimaryButton(
                    text = "Add tag",
                    onClick = {
                        val error = TagStore.add(context, newTag, newColor)
                        if (error != null) {
                            showMessage(error)
                        } else {
                            customTags = TagStore.custom(context)
                            newTag = ""
                            showMessage("Tag added")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ----- Sample data -----
            SectionCard {
                SectionTitle("Sample data")
                Text(
                    "Adds about 90 days of sample records with all readings, tags, and values both " +
                            "inside and outside your thresholds. Records are marked \"Sample\" in the remark. " +
                            "It also adds three sample medications if you have none, and sets your height to " +
                            "170 cm if it is empty. Nothing you already have is removed.",
                    style = MaterialTheme.typography.bodySmall, color = muted
                )
                SecondaryButton(
                    text = "Populate sample",
                    onClick = { confirmSample = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    editingTag?.let { t ->
        AlertDialog(
            onDismissRequest = { editingTag = null },
            title = { Text("Color for \"${t.name}\"") },
            text = {
                ColorSwatches(
                    selected = t.colorIndex,
                    onSelect = { idx ->
                        TagStore.setColor(context, t.name, idx)
                        customTags = TagStore.custom(context)
                        editingTag = t.copy(colorIndex = idx)
                    }
                )
            },
            confirmButton = { TextButton(onClick = { editingTag = null }) { Text("Done") } }
        )
    }

    if (confirmSample) {
        AlertDialog(
            onDismissRequest = { confirmSample = false },
            title = { Text("Add sample data?") },
            text = { Text("Sample records will be added next to your own. You can delete them later from History with Select all.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSample = false
                        val n = SampleData.populate(context, db)
                        showMessage("Added $n sample records")
                    }
                ) { Text("Add", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmSample = false }) { Text("Cancel") } }
        )
    }
}