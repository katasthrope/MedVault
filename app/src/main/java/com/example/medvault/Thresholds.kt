package com.example.medvault

import android.content.Context
import android.content.SharedPreferences

data class Range(val low: Double?, val high: Double?) {
    fun isOut(value: Double): Boolean =
        (low != null && value < low) || (high != null && value > high)
}

data class Thresholds(
    val glucose: Range,
    val uric: Range,
    val systolic: Range,
    val diastolic: Range
)

object ThresholdStore {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun read(p: SharedPreferences, key: String): Double? =
        p.getString(key, null)?.toDoubleOrNull()

    fun load(context: Context): Thresholds {
        val p = prefs(context)
        return Thresholds(
            glucose = Range(read(p, "glu_low"), read(p, "glu_high")),
            uric = Range(read(p, "uric_low"), read(p, "uric_high")),
            systolic = Range(read(p, "sys_low"), read(p, "sys_high")),
            diastolic = Range(read(p, "dia_low"), read(p, "dia_high"))
        )
    }

    fun save(context: Context, t: Thresholds) {
        val e = prefs(context).edit()
        fun put(key: String, v: Double?) {
            if (v == null) e.remove(key) else e.putString(key, v.toString())
        }
        put("glu_low", t.glucose.low);      put("glu_high", t.glucose.high)
        put("uric_low", t.uric.low);        put("uric_high", t.uric.high)
        put("sys_low", t.systolic.low);     put("sys_high", t.systolic.high)
        put("dia_low", t.diastolic.low);    put("dia_high", t.diastolic.high)
        e.apply()
    }
}