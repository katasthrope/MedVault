package com.example.medvault

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

data class MyReading(
    val date: String,
    val glucose: Double?,
    val uric: Double?,
    val systolic: Int?,
    val diastolic: Int?,
    val weight: Double?,
    val tag: String?,
    val remark: String
)

data class MyInsurance(val provider: String, val policyNumber: String, val notes: String)
data class MyDocument(val date: String, val name: String, val tag: String?, val note: String)

object MyData {

    // =====================================================================
    //  MY DATA TEMPLATE  -  edit the examples below, run the app, then tap
    //  Settings > Populate my data.
    //  Items already in the app are skipped, so you can tap it again later.
    //  The rows below are EXAMPLES: replace them with your own.
    // =====================================================================

    // ---------- 1. PROFILE (leave "" or null to skip a field) ----------
    val profileName = "Kevin Tantomi"
    val profileGender = "Male"            // "Male", "Female", "Other" or ""
    val profileHeightCm: Double? = 174.0

    // ---------- 2. EXTRA TAGS ----------
    // "Tag name" to "Color".  Colors: Blue, Orange, Purple, Teal, Pink, Green, Yellow, Gray
    val readingTags = listOf(
        "After exercise" to "Green"
    )
    val documentTags = listOf(
        "Dental" to "Pink"
    )

    // ---------- 3. READINGS ----------
    // reading("yyyy-MM-dd HH:mm", glucose = , uric = , systolic = , diastolic = , weight = , tag = , remark = )
    // Only the date is required, plus at least one value. Tags: Fasting, Before meal, 2h after meal, Random, or yours.
    val readings = listOf(
        reading("2026-09-18 10:45", glucose = 101.0, tag = "Fasting"),
        reading("2026-09-18 18:24", glucose = 172.0, tag = "Before meal"),
        reading("2026-09-19 07:35", glucose = 126.0, tag = "Fasting"),
        reading("2026-09-19 11:45", glucose = 148.0, tag = "Before meal"),
        reading("2026-09-19 17:30", glucose = 177.0, tag = "Before meal"),
        reading("2026-09-21 07:20", glucose = 165.0, tag = "Fasting"),
        reading("2026-09-21 11:40", glucose = 156.0, tag = "Before meal"),
        reading("2026-09-21 18:35", glucose = 91.0, tag = "Before meal"),
        reading("2026-09-28 11:56", glucose = 165.0, tag = "Random"),
        reading("2026-09-29 22:33", glucose = 207.0, tag = "2h after meal", remark = "ate seafood"),
        reading("2026-09-30 09:30", glucose = 130.0, systolic = 130, diastolic = 108, tag = "Fasting"),
        reading("2026-09-30 14:00", glucose = 209.0, systolic = 127, diastolic = 88, tag = "Before meal"),
        reading("2026-09-30 18:45", glucose = 110.0, systolic = 100, diastolic = 79, tag = "Before meal", remark = "fast walk 30 mins"),
        reading("2026-10-04 09:30", glucose = 144.0, uric = 7.0, systolic = 128, diastolic = 98, tag = "Fasting"),
        reading("2026-10-04 17:00", glucose = 75.0, systolic = 132, diastolic = 92, tag = "Before meal"),
        reading("2026-10-05 07:15", glucose = 115.0, uric = 9.0,systolic = 135, diastolic = 98, tag = "Fasting"),


    )

    // ---------- 4. MEDICATION ----------
    // medication("Name", "Dosage", "Frequency")
    val medications = listOf(
        medication("Metformin XR", "1000 mg", "Once daily"),
        medication("Allopurinol", "100 mg", "Once daily"),
        medication("Dapagliflozin", "10 mg", "Once daily"),
        medication("Atorvastatin", "20 mg", "Once daily"),
        medication("Lesichol", "600 mg", "Once daily"),
        medication("Glimepiride", "4 mg", "Once daily"),
        medication("Colchicine", "0.5 mg", "Twice daily"),
        medication("Etorix", "90 mg", "Once daily")
    )

    // ---------- 5. INSURANCE ----------
    // insurance("Provider", "Policy number", notes = "optional")
    // Upload the e-card later from Insurance > Edit.
    val insurances = listOf(
        insurance("Manulife", "4268452333", notes = "Personal"),
        insurance("Oona", "8887770028090438", notes = "HCID")
    )

    // ---------- 6. MEDICAL RECORDS ----------
    // document("yyyy-MM-dd", "Name", "Tag", note = "optional")
    // Files cannot be written in code: attach each file later from Medical records > Edit.
    // Tags: Lab test, USG, MRI, X-ray, Doctor's note, Diagnosis, Prescription, Invoice, Other, or yours.
    val documents = listOf(
        document("2026-09-20", "Sample Document", "Lab test", note = "Sample Note")
    )

    // =====================================================================
    //  Do not edit below this line
    // =====================================================================

    private fun reading(
        date: String,
        glucose: Double? = null,
        uric: Double? = null,
        systolic: Int? = null,
        diastolic: Int? = null,
        weight: Double? = null,
        tag: String? = null,
        remark: String = ""
    ) = MyReading(date, glucose, uric, systolic, diastolic, weight, tag, remark)

    private fun medication(name: String, dosage: String, frequency: String) =
        Medication(name = name, dosage = dosage, frequency = frequency)

    private fun insurance(provider: String, policy: String, notes: String = "") =
        MyInsurance(provider, policy, notes)

    private fun document(date: String, name: String, tag: String?, note: String = "") =
        MyDocument(date, name, tag, note)

    /** Adds anything from the template that is not already in the app. Returns a summary message. */
    fun populate(context: Context, db: HealthDb): String {
        val dtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        val dFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val problems = mutableListOf<String>()
        var skipped = 0

        // Profile
        val p = ProfileStore.load(context)
        ProfileStore.save(
            context,
            p.copy(
                name = if (profileName.isNotBlank()) profileName else p.name,
                gender = if (profileGender.isNotBlank()) profileGender else p.gender,
                heightCm = profileHeightCm ?: p.heightCm
            )
        )

        // Extra tags (an error just means the tag already exists)
        readingTags.forEach { (n, c) -> TagStore.add(context, n, TagPalette.indexOf(c)) }
        documentTags.forEach { (n, c) -> DocTagStore.add(context, n, TagPalette.indexOf(c)) }

        // Readings
        val seenTs = db.getAll().map { it.timestamp }.toMutableSet()
        val newReadings = mutableListOf<LogEntry>()
        readings.forEach { r ->
            val ts = try { dtFmt.parse(r.date)?.time } catch (e: Exception) { null }
            val hasValue = r.glucose != null || r.uric != null || r.systolic != null || r.weight != null
            when {
                ts == null -> problems.add("reading date '${r.date}'")
                !hasValue -> problems.add("reading ${r.date} has no values")
                (r.systolic == null) != (r.diastolic == null) ->
                    problems.add("reading ${r.date} needs both systolic and diastolic")
                !seenTs.add(ts) -> skipped++
                else -> newReadings.add(
                    LogEntry(
                        timestamp = ts,
                        glucose = r.glucose,
                        uricAcid = r.uric,
                        systolic = r.systolic,
                        diastolic = r.diastolic,
                        remark = r.remark,
                        tag = r.tag,
                        weight = r.weight
                    )
                )
            }
        }
        db.insertMany(newReadings)

        // Medication
        val haveMeds = db.getMedications().map { it.name.lowercase() }.toMutableSet()
        var medsAdded = 0
        medications.forEach { m ->
            if (haveMeds.add(m.name.lowercase())) {
                db.insertMedication(m)
                medsAdded++
            } else skipped++
        }

        // Insurance
        val havePolicies = db.getInsurances().map { it.policyNumber.lowercase() }.toMutableSet()
        var insAdded = 0
        insurances.forEach { i ->
            if (havePolicies.add(i.policyNumber.lowercase())) {
                db.insertInsurance(
                    Insurance(provider = i.provider, policyNumber = i.policyNumber, notes = i.notes, ecard = null, ecardMime = null)
                )
                insAdded++
            } else skipped++
        }

        // Medical records
        val haveDocs = db.getDocuments().map { it.name.lowercase() + "|" + it.dateMs }.toMutableSet()
        var docsAdded = 0
        documents.forEach { d ->
            val ts = try { dFmt.parse(d.date)?.time } catch (e: Exception) { null }
            if (ts == null) {
                problems.add("document date '${d.date}'")
            } else if (haveDocs.add(d.name.lowercase() + "|" + ts)) {
                db.insertDocument(Document(name = d.name, tag = d.tag, dateMs = ts, note = d.note, file = null, mime = null))
                docsAdded++
            } else skipped++
        }

        val sb = StringBuilder("Added ${newReadings.size} readings, $medsAdded medication, $insAdded insurance, $docsAdded documents.")
        if (skipped > 0) sb.append(" Skipped $skipped already present.")
        if (problems.isNotEmpty()) sb.append(" Check: ${problems.take(3).joinToString("; ")}.")
        return sb.toString()
    }
}