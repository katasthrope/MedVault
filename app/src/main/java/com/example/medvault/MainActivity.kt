package com.example.medvault

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = HealthDb(applicationContext)
        setContent {
            val prefs = remember { getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark", false)) }

            DisposableEffect(darkTheme) {
                val bars = if (darkTheme) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = bars,
                    navigationBarStyle = bars
                )
                onDispose {}
            }

            AppTheme(darkTheme) {
                App(
                    db = db,
                    darkTheme = darkTheme,
                    onThemeChange = { dark ->
                        darkTheme = dark
                        prefs.edit().putBoolean("dark", dark).apply()
                    }
                )
            }
        }
    }
}