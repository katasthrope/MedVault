package com.example.medvault

import android.content.Context
import java.util.Calendar
import java.util.Random
import kotlin.math.sin

object SampleData {
    private fun r1(v: Double) = Math.round(v * 10) / 10.0

    /** Adds ~90 days of sample records. Returns how many records were added. */
    fun populate(context: Context, db: HealthDb): Int {
        val rnd = Random()
        val now = System.currentTimeMillis()
        val customTags = TagStore.custom(context).map { it.name }
        val slots = listOf(7 to "Fasting", 12 to "Before meal", 14 to "2h after meal", 20 to "Random")
        val list = mutableListOf<LogEntry>()

        for (daysAgo in 90 downTo 0) {
            if (rnd.nextInt(100) < 12) continue // a few days with no records
            val todays = slots.shuffled(rnd).take(1 + rnd.nextInt(3)).sortedBy { it.first }

            todays.forEachIndexed { index, (hour, presetTag) ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, rnd.nextInt(45))
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis > now) return@forEachIndexed

                val tag = if (customTags.isNotEmpty() && rnd.nextInt(100) < 15)
                    customTags[rnd.nextInt(customTags.size)] else presetTag

                val base = when (presetTag) {
                    "Fasting" -> 78 + rnd.nextInt(40)
                    "Before meal" -> 82 + rnd.nextInt(50)
                    "2h after meal" -> 105 + rnd.nextInt(85)
                    else -> 70 + rnd.nextInt(90)
                }
                val g = if (rnd.nextInt(100) < 8) (56 + rnd.nextInt(14)).toDouble() else base.toDouble()
                val glucose: Double? = if (rnd.nextInt(100) < 90) g else null
                val uric: Double? = if (rnd.nextInt(100) < 35) r1(4.0 + rnd.nextDouble() * 4.4) else null
                val hasBp = rnd.nextInt(100) < 55
                val sys: Int? = if (hasBp) 100 + rnd.nextInt(50) else null
                val dia: Int? = if (hasBp) 62 + rnd.nextInt(36) else null
                val weight: Double? =
                    if (index == 0 && daysAgo % 3 == 0)
                        r1(72.0 + 2.5 * sin(daysAgo / 14.0) + (rnd.nextDouble() - 0.5) * 0.8)
                    else null

                val hasAny = glucose != null || uric != null || sys != null || weight != null
                list.add(
                    LogEntry(
                        timestamp = cal.timeInMillis,
                        glucose = if (hasAny) glucose else g,
                        uricAcid = uric,
                        systolic = sys,
                        diastolic = dia,
                        remark = "Sample",
                        tag = tag,
                        weight = weight
                    )
                )
            }
        }
        db.insertMany(list)

        if (db.getMedications().isEmpty()) {
            db.insertMedication(Medication(name = "Vitamin D3", dosage = "1000 IU", frequency = "Once daily"))
            db.insertMedication(Medication(name = "Omega-3", dosage = "1 capsule", frequency = "Once daily"))
            db.insertMedication(Medication(name = "Multivitamin", dosage = "1 tablet", frequency = "Once daily"))
        }

        val profile = ProfileStore.load(context)
        if (profile.heightCm == null) ProfileStore.save(context, profile.copy(heightCm = 170.0))

        return list.size
    }
}