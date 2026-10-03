package com.example.medvault

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onLog: () -> Unit,
    onView: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Settings
        IconButton(
            onClick = onSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("⚙️", style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MedVault Karubii", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(40.dp))
            Button(onClick = onLog, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("New Log")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onView, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("History")
            }
        }
    }
}