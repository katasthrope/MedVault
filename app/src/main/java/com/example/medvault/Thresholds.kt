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
    val diastolic: Range,
    val weight: Range = Range(null, null),
    val bmi: Range = Range(18.5, 24.9)
) {
    companion object {
        // General adult reference values. Adjust in Settings to match your doctor's advice.
        val DEFAULT = Thresholds(
            glucose = Range(70.0, 140.0),
            uric = Range(2.5, 7.0),
            systolic = Range(90.0, 120.0),
            diastolic = Range(60.0, 80.0),
            weight = Range(null, null),
            bmi = Range(18.5, 24.9)
        )
    }
}

object ThresholdStore {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun read(p: SharedPreferences, key: String): Double? =
        p.getString(key, null)?.toDoubleOrNull()

    fun load(context: Context): Thresholds {
        val p = prefs(context)
        val everSaved = p.getBoolean("thresholds_saved", false) ||
                p.all.keys.any { it.endsWith("_low") || it.endsWith("_high") }
        if (!everSaved) return Thresholds.DEFAULT

        // BMI limits added later: use the default until the user saves them once
        val bmiSaved = p.getBoolean("bmi_saved", false)
        return Thresholds(
            glucose = Range(read(p, "glu_low"), read(p, "glu_high")),
            uric = Range(read(p, "uric_low"), read(p, "uric_high")),
            systolic = Range(read(p, "sys_low"), read(p, "sys_high")),
            diastolic = Range(read(p, "dia_low"), read(p, "dia_high")),
            weight = Range(read(p, "wt_low"), read(p, "wt_high")),
            bmi = if (bmiSaved) Range(read(p, "bmi_low"), read(p, "bmi_high")) else Thresholds.DEFAULT.bmi
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
        put("wt_low", t.weight.low);        put("wt_high", t.weight.high)
        put("bmi_low", t.bmi.low);          put("bmi_high", t.bmi.high)
        e.putBoolean("thresholds_saved", true)
        e.putBoolean("bmi_saved", true)
        e.apply()
    }
}