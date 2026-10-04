package com.example.medvault

import java.util.Locale

enum class Metric(val label: String, val unit: String) {
    GLUCOSE("Glucose", "mg/dL"),
    URIC("Uric acid", "mg/dL"),
    SYSTOLIC("Systolic", "mmHg"),
    DIASTOLIC("Diastolic", "mmHg"),
    WEIGHT("Weight", "kg"),
    BMI("BMI", "kg/m²")
}

fun bmiOf(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val m = heightCm / 100.0
    return weightKg / (m * m)
}

fun Metric.readingOf(e: LogEntry, heightCm: Double?): Double? = when (this) {
    Metric.GLUCOSE -> e.glucose
    Metric.URIC -> e.uricAcid
    Metric.SYSTOLIC -> e.systolic?.toDouble()
    Metric.DIASTOLIC -> e.diastolic?.toDouble()
    Metric.WEIGHT -> e.weight
    Metric.BMI -> bmiOf(e.weight, heightCm)
}

fun Metric.rangeOf(t: Thresholds): Range = when (this) {
    Metric.GLUCOSE -> t.glucose
    Metric.URIC -> t.uric
    Metric.SYSTOLIC -> t.systolic
    Metric.DIASTOLIC -> t.diastolic
    Metric.WEIGHT -> t.weight
    Metric.BMI -> t.bmi
}

fun Metric.display(v: Double): String =
    if (this == Metric.BMI) String.format(Locale.US, "%.1f", v) else fmt(v)