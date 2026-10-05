package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManagerCard(title: String, catalog: TagCatalog, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var custom by remember { mutableStateOf(catalog.custom(context)) }
    var newTag by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(TagPalette.DEFAULT_CUSTOM) }
    var editing by remember { mutableStateOf<Tag?>(null) }

    SectionCard {
        SectionTitle(title)

        Text("Built-in", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            catalog.presets.forEach { TagPill(it.name, it.colorIndex) }
        }

        Text("Your tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (custom.isEmpty()) {
            Text("No custom tags yet.", style = MaterialTheme.typography.bodySmall, color = muted)
        } else {
            Text("Tap a color circle to change its color.", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        custom.forEach { tag ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TagPalette.get(tag.colorIndex).base)
                        .clickable { editing = tag }
                )
                Spacer(Modifier.width(12.dp))
                Text(tag.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        catalog.remove(context, tag.name)
                        custom = catalog.custom(context)
                        showMessage("Tag removed. Existing items keep it.")
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
                val error = catalog.add(context, newTag, newColor)
                if (error != null) {
                    showMessage(error)
                } else {
                    custom = catalog.custom(context)
                    newTag = ""
                    showMessage("Tag added")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    editing?.let { t ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Color for \"${t.name}\"") },
            text = {
                ColorSwatches(
                    selected = t.colorIndex,
                    onSelect = { idx ->
                        catalog.setColor(context, t.name, idx)
                        custom = catalog.custom(context)
                        editing = t.copy(colorIndex = idx)
                    }
                )
            },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("Done") } }
        )
    }
}