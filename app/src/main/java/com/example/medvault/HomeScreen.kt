package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun ActionCard(
    icon: String,
    title: String,
    subtitle: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    val container = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(22.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(content.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = content
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.8f)
            )
        }
        Text("›", style = MaterialTheme.typography.headlineMedium, color = content)
    }
}

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onLog: () -> Unit,
    onView: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "MedVault",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Your readings, always at hand",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(36.dp))

        ActionCard(
            icon = "📝",
            title = "New Log",
            subtitle = "Record glucose, uric acid and blood pressure",
            filled = true,
            onClick = onLog
        )
        Spacer(Modifier.height(16.dp))
        ActionCard(
            icon = "📈",
            title = "History",
            subtitle = "Browse, filter and manage your records",
            filled = false,
            onClick = onView
        )
    }
}