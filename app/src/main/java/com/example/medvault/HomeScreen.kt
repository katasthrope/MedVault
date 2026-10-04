package com.example.medvault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(content.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = content)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
        }
        Text("›", style = MaterialTheme.typography.headlineMedium, color = content)
    }
}

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onLog: () -> Unit,
    onView: () -> Unit,
    onDashboard: () -> Unit,
    onMedication: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val photo = remember { ProfileStore.loadPhoto(context) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                photo, profile.name, 52.dp,
                Modifier
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(onClick = onProfile)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "MedVault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (profile.name.isNotBlank()) "Hello, ${profile.name}" else "Your readings, always at hand",
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

        Spacer(Modifier.height(28.dp))

        ActionCard("📝", "New Log", "Record glucose, uric acid, blood pressure, weight", true, onLog)
        Spacer(Modifier.height(14.dp))
        ActionCard("📈", "History", "Browse, filter and manage your records", false, onView)
        Spacer(Modifier.height(14.dp))
        ActionCard("📊", "Dashboard", "Latest, min and max, charts and heatmaps", false, onDashboard)
        Spacer(Modifier.height(14.dp))
        ActionCard("💊", "Medication", "What you are currently taking", false, onMedication)
        Spacer(Modifier.height(8.dp))
    }
}