package com.example.medvault

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random
import kotlin.math.sin

object SampleData {
    private const val MARK = "Sample"
    private const val DAYS = 180

    private val SAMPLE_MEDS = listOf(
        Medication(name = "Vitamin D3", dosage = "1000 IU", frequency = "Once daily"),
        Medication(name = "Omega-3", dosage = "1 capsule", frequency = "Once daily"),
        Medication(name = "Multivitamin", dosage = "1 tablet", frequency = "Once daily")
    )

    // name, document tag, days ago
    private val SAMPLE_DOCS = listOf(
        Triple("Complete blood count", "Lab test", 172),
        Triple("Abdominal ultrasound (USG)", "USG", 150),
        Triple("Knee MRI report", "MRI", 131),
        Triple("Chest X-ray", "X-ray", 118),
        Triple("Cardiology consultation note", "Doctor's note", 96),
        Triple("Diagnosis summary", "Diagnosis", 95),
        Triple("Prescription - 3 months", "Prescription", 80),
        Triple("Hospital invoice", "Invoice", 64),
        Triple("HbA1c and lipid panel", "Lab test", 45),
        Triple("Follow-up consultation", "Doctor's note", 30),
        Triple("Pharmacy invoice", "Invoice", 12),
        Triple("Annual check-up report", "Other", 5)
    )

    // provider, policy number, card color
    private val SAMPLE_INS = listOf(
        Triple("Sample Health Insurance", "SH-2026-48213", "#00796B"),
        Triple("Sample Dental Plan", "DP-9981-2207", "#5E35B1")
    )

    private fun r1(v: Double) = Math.round(v * 10) / 10.0

    /** Replaces any earlier sample data with fresh sample data. Returns a summary message. */
    fun populate(context: Context, db: HealthDb): String {
        remove(context, db)

        val rnd = Random()
        val now = System.currentTimeMillis()
        val customTags = TagStore.custom(context).map { it.name }
        val slots = listOf(7 to "Fasting", 12 to "Before meal", 14 to "2h after meal", 20 to "Random")
        val list = mutableListOf<LogEntry>()

        for (daysAgo in DAYS downTo 0) {
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
                        remark = MARK,
                        tag = tag,
                        weight = weight
                    )
                )
            }
        }
        db.insertMany(list)

        // Medications (only if none yet)
        if (db.getMedications().isEmpty()) SAMPLE_MEDS.forEach { db.insertMedication(it) }

        // Height, so BMI works
        val profile = ProfileStore.load(context)
        if (profile.heightCm == null) ProfileStore.save(context, profile.copy(heightCm = 170.0))

        // Medical records with generated placeholder pages
        val docColors = DocTagStore.colorMap(context)
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        SAMPLE_DOCS.forEach { (name, tag, daysAgo) ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val accent = TagPalette.get(docColors[tag] ?: TagPalette.FALLBACK).base.toArgb()
            val file = FileStore.makeSampleDocument(
                context, "docs", name, "${dateFmt.format(cal.time)}  |  $tag", accent
            )
            db.insertDocument(
                Document(name = name, tag = tag, dateMs = cal.timeInMillis, note = MARK, file = file, mime = "image/jpeg")
            )
        }

        // Insurance with generated e-cards
        SAMPLE_INS.forEach { (provider, policy, color) ->
            val card = FileStore.makeSampleCard(context, "ecards", provider, policy, Color.parseColor(color))
            db.insertInsurance(
                Insurance(provider = provider, policyNumber = policy, notes = MARK, ecard = card, ecardMime = "image/jpeg")
            )
        }

        return "Added ${list.size} sample records, ${SAMPLE_DOCS.size} documents and ${SAMPLE_INS.size} insurances ($DAYS days)"
    }

    /** Removes only the sample items. Returns a summary message. */
    fun remove(context: Context, db: HealthDb): String {
        val records = db.deleteLogsWithRemark(MARK)

        var docs = 0
        db.getDocuments().filter { it.note == MARK }.forEach {
            FileStore.delete(context, "docs", it.file)
            db.deleteDocument(it.id)
            docs++
        }

        var ins = 0
        db.getInsurances().filter { it.notes == MARK }.forEach {
            FileStore.delete(context, "ecards", it.ecard)
            db.deleteInsurance(it.id)
            ins++
        }

        db.getMedications().filter { it.copy(id = 0) in SAMPLE_MEDS }.forEach { db.deleteMedication(it.id) }

        return "Removed $records sample records, $docs documents and $ins insurances"
    }
}