package com.example.medvault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun App(db: HealthDb, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    var screen by remember { mutableStateOf("home") }
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    BackHandler(enabled = screen != "home") {
        screen = if (screen == "edit") "view" else "home"
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                "log" -> LogScreen(
                    db = db,
                    existing = null,
                    onBack = { screen = "home" },
                    onSaved = {},
                    showMessage = showMessage
                )
                "edit" -> LogScreen(
                    db = db,
                    existing = editing,
                    onBack = { screen = "view" },
                    onSaved = { screen = "view" },
                    showMessage = showMessage
                )
                "view" -> ViewScreen(
                    db = db,
                    onBack = { screen = "home" },
                    onEdit = { editing = it; screen = "edit" }
                )
                "dashboard" -> DashboardScreen(db = db, onBack = { screen = "home" })
                "medication" -> MedicationScreen(
                    db = db,
                    onBack = { screen = "home" },
                    showMessage = showMessage
                )
                "profile" -> ProfileScreen(
                    db = db,
                    onBack = { screen = "home" },
                    showMessage = showMessage
                )
                "settings" -> SettingsScreen(
                    db = db,
                    darkTheme = darkTheme,
                    onThemeChange = onThemeChange,
                    onBack = { screen = "home" },
                    showMessage = showMessage
                )
                else -> HomeScreen(
                    onSettings = { screen = "settings" },
                    onProfile = { screen = "profile" },
                    onLog = { screen = "log" },
                    onView = { screen = "view" },
                    onDashboard = { screen = "dashboard" },
                    onMedication = { screen = "medication" }
                )
            }
        }
    }
}