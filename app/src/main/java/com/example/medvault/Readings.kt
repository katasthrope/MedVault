package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun mutedColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

@Composable
fun okColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

@Composable
fun flagged(out: Boolean): SpanStyle =
    if (out) SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    else SpanStyle(fontWeight = FontWeight.SemiBold)

@Composable
fun valueText(text: String, out: Boolean): AnnotatedString {
    val style = flagged(out)
    return buildAnnotatedString { withStyle(style) { append(text) } }
}

@Composable
fun MetricTile(
    label: String,
    unit: String,
    value: AnnotatedString?,
    out: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (out) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val muted = mutedColor()

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        } else {
            Text("–", style = MaterialTheme.typography.titleMedium, color = muted)
        }
        Text(unit, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
    }
}

/** Glucose / uric acid / blood pressure tiles, plus a weight + BMI row when a weight was logged. */
@Composable
fun ReadingTiles(e: LogEntry, t: Thresholds, heightCm: Double?) {
    val gOut = e.glucose?.let { t.glucose.isOut(it) } ?: false
    val uOut = e.uricAcid?.let { t.uric.isOut(it) } ?: false
    val sysOut = e.systolic?.let { t.systolic.isOut(it.toDouble()) } ?: false
    val diaOut = e.diastolic?.let { t.diastolic.isOut(it.toDouble()) } ?: false
    val wOut = e.weight?.let { t.weight.isOut(it) } ?: false
    val bmi = bmiOf(e.weight, heightCm)
    val bOut = bmi?.let { t.bmi.isOut(it) } ?: false

    val sysStyle = flagged(sysOut)
    val diaStyle = flagged(diaOut)
    val plainStyle = flagged(false)
    val bpText: AnnotatedString? =
        if (e.systolic != null && e.diastolic != null) {
            buildAnnotatedString {
                withStyle(sysStyle) { append("${e.systolic}") }
                withStyle(plainStyle) { append("/") }
                withStyle(diaStyle) { append("${e.diastolic}") }
            }
        } else null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Glucose", "mg/dL", e.glucose?.let { valueText(fmt(it), gOut) }, gOut, Modifier.weight(1f))
            MetricTile("Uric acid", "mg/dL", e.uricAcid?.let { valueText(fmt(it), uOut) }, uOut, Modifier.weight(1f))
            MetricTile("Blood pressure", "mmHg", bpText, sysOut || diaOut, Modifier.weight(1.4f))
        }
        if (e.weight != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Weight", "kg", valueText(fmt(e.weight), wOut), wOut, Modifier.weight(1f))
                MetricTile(
                    "BMI", "kg/m²",
                    bmi?.let { valueText(Metric.BMI.display(it), bOut) },
                    bOut, Modifier.weight(1f)
                )
                Spacer(Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🗒️", style = MaterialTheme.typography.headlineLarge)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = mutedColor(),
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}