package com.example.medvault

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

fun formatTime(ms: Long): String = dateFormat.format(Date(ms))

fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

fun pickDateTime(context: Context, initial: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            TimePickerDialog(
                context,
                { _, h, min ->
                    val c = Calendar.getInstance()
                    c.set(y, m, d, h, min, 0)
                    onPicked(c.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}