package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

    var confirmSample by remember { mutableStateOf(false) }
    var confirmRemoveSample by remember { mutableStateOf(false) }
    var confirmMyData by remember { mutableStateOf(false) }

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
            TagManagerCard("Reading tags", TagStore, showMessage)
            TagManagerCard("Document tags", DocTagStore, showMessage)

            // ----- Sample data -----
            SectionCard {
                SectionTitle("Sample data")
                Text(
                    "Fills the app with 180 days of sample readings (inside and outside your thresholds, " +
                            "with tags), 12 medical records with generated pages, 2 insurances with e-cards, " +
                            "and 3 medications if you have none. It sets your height to 170 cm if empty. " +
                            "Tapping it again replaces the earlier sample data. Your own items are not touched.",
                    style = MaterialTheme.typography.bodySmall, color = muted
                )
                SecondaryButton(
                    text = "Populate sample",
                    onClick = { confirmSample = true },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(
                    text = "Remove sample data",
                    onClick = { confirmRemoveSample = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ----- My data -----
            SectionCard {
                SectionTitle("My data")
                Text(
                    "Loads your own data from the template file MyData.kt in the project: profile, " +
                            "extra tags, readings, medication, insurance and medical record entries. " +
                            "Edit the file in Android Studio, run the app, then tap the button. " +
                            "Items already in the app are skipped, so it is safe to tap more than once.",
                    style = MaterialTheme.typography.bodySmall, color = muted
                )
                PrimaryButton(
                    text = "Populate my data",
                    onClick = { confirmMyData = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmSample) {
        AlertDialog(
            onDismissRequest = { confirmSample = false },
            title = { Text("Add sample data?") },
            text = { Text("Earlier sample data is replaced. Your own records, documents and insurances stay.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSample = false
                        showMessage(SampleData.populate(context, db))
                    }
                ) { Text("Add", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmSample = false }) { Text("Cancel") } }
        )
    }

    if (confirmRemoveSample) {
        AlertDialog(
            onDismissRequest = { confirmRemoveSample = false },
            title = { Text("Remove sample data?") },
            text = { Text("Only the sample records, documents, insurances and sample medications are deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoveSample = false
                        showMessage(SampleData.remove(context, db))
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveSample = false }) { Text("Cancel") } }
        )
    }

    if (confirmMyData) {
        AlertDialog(
            onDismissRequest = { confirmMyData = false },
            title = { Text("Populate my data?") },
            text = { Text("Adds everything in MyData.kt that is not already in the app. Nothing is deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmMyData = false
                        showMessage(MyData.populate(context, db))
                    }
                ) { Text("Populate", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmMyData = false }) { Text("Cancel") } }
        )
    }
}