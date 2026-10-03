package com.example.medvault

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

class TagColor(val name: String, val base: Color, val onLight: Color, val onDark: Color)

object TagPalette {
    val colors = listOf(
        TagColor("Blue", Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF90CAF9)),
        TagColor("Orange", Color(0xFFFF9800), Color(0xFFE65100), Color(0xFFFFB74D)),
        TagColor("Purple", Color(0xFF9C27B0), Color(0xFF6A1B9A), Color(0xFFCE93D8)),
        TagColor("Teal", Color(0xFF009688), Color(0xFF00695C), Color(0xFF80CBC4)),
        TagColor("Pink", Color(0xFFE91E63), Color(0xFFAD1457), Color(0xFFF48FB1)),
        TagColor("Green", Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFFA5D6A7)),
        TagColor("Yellow", Color(0xFFFFC107), Color(0xFF8D6E00), Color(0xFFFFE082)),
        TagColor("Gray", Color(0xFF607D8B), Color(0xFF455A64), Color(0xFFB0BEC5))
    )
    const val DEFAULT_CUSTOM = 3   // Teal
    const val FALLBACK = 7         // Gray

    fun get(index: Int): TagColor = colors[index.coerceIn(0, colors.lastIndex)]
}

/** Readable color for tag text/dots in the current (light or dark) theme. */
@Composable
fun tagAccent(index: Int): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val c = TagPalette.get(index)
    return if (dark) c.onDark else c.onLight
}

data class Tag(val name: String, val colorIndex: Int)

object TagStore {
    val PRESETS = listOf(
        Tag("Fasting", 0),
        Tag("Before meal", 1),
        Tag("2h after meal", 2),
        Tag("Random", 7)
    )

    private const val KEY = "custom_tags"
    private const val SEP = "|"

    private fun prefs(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Stored as one line per tag: name|colorIndex (older lines without a color get teal)
    fun custom(context: Context): List<Tag> {
        val raw = prefs(context).getString(KEY, "") ?: ""
        return raw.split("\n").mapNotNull { line ->
            val t = line.trim()
            if (t.isEmpty()) {
                null
            } else {
                val parts = t.split(SEP)
                val name = parts[0].trim()
                val color = parts.getOrNull(1)?.toIntOrNull() ?: TagPalette.DEFAULT_CUSTOM
                if (name.isEmpty()) null else Tag(name, color)
            }
        }
    }

    fun all(context: Context): List<Tag> = PRESETS + custom(context)

    fun colorMap(context: Context): Map<String, Int> =
        all(context).associate { it.name to it.colorIndex }

    private fun saveCustom(context: Context, list: List<Tag>) {
        prefs(context).edit()
            .putString(KEY, list.joinToString("\n") { "${it.name}$SEP${it.colorIndex}" })
            .apply()
    }

    /** Returns an error message, or null if the tag was added. */
    fun add(context: Context, raw: String, colorIndex: Int): String? {
        val name = raw.trim().replace("\n", " ").replace(SEP, " ")
        if (name.isEmpty()) return "Type a tag name first"
        if (name.length > 24) return "Tag name is too long (max 24 characters)"
        if (all(context).any { it.name.equals(name, ignoreCase = true) }) return "That tag already exists"
        saveCustom(context, custom(context) + Tag(name, colorIndex))
        return null
    }

    fun remove(context: Context, name: String) {
        saveCustom(context, custom(context).filter { it.name != name })
    }

    fun setColor(context: Context, name: String, colorIndex: Int) {
        saveCustom(context, custom(context).map {
            if (it.name == name) it.copy(colorIndex = colorIndex) else it
        })
    }
}