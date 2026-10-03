package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private fun parseNumber(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()

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
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = low,
                onValueChange = onLow,
                label = { Text("Low") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = high,
                onValueChange = onHigh,
                label = { Text("High") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { ThresholdStore.load(context) }

    var gluLow by remember { mutableStateOf(saved.glucose.low?.let { fmt(it) } ?: "") }
    var gluHigh by remember { mutableStateOf(saved.glucose.high?.let { fmt(it) } ?: "") }
    var uricLow by remember { mutableStateOf(saved.uric.low?.let { fmt(it) } ?: "") }
    var uricHigh by remember { mutableStateOf(saved.uric.high?.let { fmt(it) } ?: "") }
    var sysLow by remember { mutableStateOf(saved.systolic.low?.let { fmt(it) } ?: "") }
    var sysHigh by remember { mutableStateOf(saved.systolic.high?.let { fmt(it) } ?: "") }
    var diaLow by remember { mutableStateOf(saved.diastolic.low?.let { fmt(it) } ?: "") }
    var diaHigh by remember { mutableStateOf(saved.diastolic.high?.let { fmt(it) } ?: "") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        // ----- Appearance -----
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("☀️")
            Spacer(Modifier.width(8.dp))
            Switch(checked = darkTheme, onCheckedChange = onThemeChange)
            Spacer(Modifier.width(8.dp))
            Text("🌙")
        }

        Spacer(Modifier.height(16.dp))

        // ----- Thresholds -----
        Text("Thresholds", style = MaterialTheme.typography.titleMedium)
        Text(
            "Values below Low or above High appear in red in History. " +
                    "Leave a field empty for no limit.",
            style = MaterialTheme.typography.bodySmall
        )

        RangeRow("Glucose (mg/dL)", gluLow, { gluLow = it }, gluHigh, { gluHigh = it })
        RangeRow("Uric acid (mg/dL)", uricLow, { uricLow = it }, uricHigh, { uricHigh = it })
        RangeRow("Systolic (mmHg)", sysLow, { sysLow = it }, sysHigh, { sysHigh = it })
        RangeRow("Diastolic (mmHg)", diaLow, { diaLow = it }, diaHigh, { diaHigh = it })

        Button(
            onClick = {
                val error = problem("Glucose", gluLow, gluHigh)
                    ?: problem("Uric acid", uricLow, uricHigh)
                    ?: problem("Systolic", sysLow, sysHigh)
                    ?: problem("Diastolic", diaLow, diaHigh)
                if (error != null) {
                    showMessage(error)
                } else {
                    ThresholdStore.save(
                        context,
                        Thresholds(
                            glucose = Range(parseNumber(gluLow), parseNumber(gluHigh)),
                            uric = Range(parseNumber(uricLow), parseNumber(uricHigh)),
                            systolic = Range(parseNumber(sysLow), parseNumber(sysHigh)),
                            diastolic = Range(parseNumber(diaLow), parseNumber(diaHigh))
                        )
                    )
                    showMessage("Thresholds saved")
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Save thresholds")
        }
    }
}