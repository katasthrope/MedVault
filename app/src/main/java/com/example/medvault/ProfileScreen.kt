package com.example.medvault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun Avatar(photo: ImageBitmap?, name: String, size: Dp, modifier: Modifier = Modifier) {
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val initial = name.trim().firstOrNull()?.uppercase() ?: "👤"
            Text(
                initial,
                fontSize = (size.value / 2.4f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BmiBadge(bmi: Double?, range: Range) {
    val low = range.low
    val high = range.high
    val status = when {
        bmi == null -> "No data"
        low != null && bmi < low -> "Below range"
        high != null && bmi > high -> "Above range"
        else -> "In range"
    }
    val color = when {
        bmi == null -> mutedColor()
        range.isOut(bmi) -> MaterialTheme.colorScheme.error
        low == null && high == null -> MaterialTheme.colorScheme.primary
        else -> okColor()
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.5.dp, color, shape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BMI", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        Text(
            if (bmi != null) Metric.BMI.display(bmi) else "–",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(status, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun ProfileScreen(db: HealthDb, onBack: () -> Unit, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thresholds = remember { ThresholdStore.load(context) }
    val saved = remember { ProfileStore.load(context) }
    val latest = remember { db.getAll().firstOrNull { it.weight != null } }

    var name by remember { mutableStateOf(saved.name) }
    var gender by remember { mutableStateOf(saved.gender) }
    var heightText by remember { mutableStateOf(saved.heightCm?.let { fmt(it) } ?: "") }
    var useOverride by remember { mutableStateOf(saved.useOverride) }
    var overrideText by remember { mutableStateOf(saved.weightOverride?.let { fmt(it) } ?: "") }
    var photo by remember { mutableStateOf(ProfileStore.loadPhoto(context)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { ProfileStore.savePhoto(context, uri) }
                if (ok) {
                    photo = withContext(Dispatchers.IO) { ProfileStore.loadPhoto(context) }
                    showMessage("Photo updated")
                } else {
                    showMessage("Could not use that picture")
                }
            }
        }
    }

    // Weight and BMI are computed live from the form
    val height = heightText.trim().replace(',', '.').toDoubleOrNull()
    val overrideW = overrideText.trim().replace(',', '.').toDoubleOrNull()
    val loggedW = latest?.weight
    val weight = if (useOverride && overrideW != null) overrideW else loggedW
    val bmi = bmiOf(weight, height)

    val source = when {
        useOverride && overrideW != null -> "Manual override"
        loggedW != null && latest != null -> "From your latest weight log (${shortDateFormat.format(Date(latest.timestamp))})"
        else -> "No weight logged yet"
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Profile", onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(photo, name, 112.dp)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { picker.launch("image/*") }) {
                            Text(if (photo == null) "Add photo" else "Change photo", fontWeight = FontWeight.SemiBold)
                        }
                        if (photo != null) {
                            TextButton(onClick = {
                                ProfileStore.removePhoto(context)
                                photo = null
                            }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
                Text("Gender", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        TagChip(g, null, gender == g) { gender = if (gender == g) "" else g }
                    }
                }
                OutlinedTextField(
                    value = heightText, onValueChange = { heightText = it },
                    label = { Text("Height") },
                    suffix = { Text("cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard {
                SectionTitle("Weight and BMI")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weight", style = MaterialTheme.typography.labelMedium, color = mutedColor())
                        Text(
                            if (weight != null) "${fmt(weight)} kg" else "–",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(source, style = MaterialTheme.typography.bodySmall, color = mutedColor())
                    }
                    BmiBadge(bmi, thresholds.bmi)
                }
                if (height == null) {
                    Text(
                        "Enter your height to see your BMI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Override weight", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "BMI is always calculated automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor()
                        )
                    }
                    Switch(checked = useOverride, onCheckedChange = { useOverride = it })
                }
                if (useOverride) {
                    OutlinedTextField(
                        value = overrideText, onValueChange = { overrideText = it },
                        label = { Text("Weight") },
                        suffix = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, shape = FieldShape, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PrimaryButton(
            text = "Save profile",
            onClick = {
                val hText = heightText.trim().replace(',', '.')
                val hv = hText.toDoubleOrNull()
                when {
                    hText.isNotEmpty() && (hv == null || hv < 50 || hv > 250) ->
                        showMessage("Height should be between 50 and 250 cm")
                    useOverride && (overrideW == null || overrideW <= 0.0) ->
                        showMessage("Enter a valid override weight, or turn the override off")
                    else -> {
                        ProfileStore.save(
                            context,
                            Profile(
                                name = name.trim(),
                                gender = gender,
                                heightCm = hv,
                                weightOverride = if (useOverride) overrideW else saved.weightOverride,
                                useOverride = useOverride
                            )
                        )
                        showMessage("Profile saved")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}