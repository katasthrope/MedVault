package com.example.medvault

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String
)

data class Document(
    val id: Long = 0,
    val name: String,
    val tag: String?,
    val dateMs: Long,
    val note: String,
    val file: String?,
    val mime: String?
)

data class Insurance(
    val id: Long = 0,
    val provider: String,
    val policyNumber: String,
    val notes: String,
    val ecard: String?,
    val ecardMime: String?
)

class HealthDb(context: Context) :
    SQLiteOpenHelper(context, "health.db", null, 5) {

    private val createSql = """CREATE TABLE logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                glucose REAL,
                uric REAL,
                systolic INTEGER,
                diastolic INTEGER,
                remark TEXT NOT NULL,
                tag TEXT,
                weight REAL
            )"""

    // Layout of version 2, used only when upgrading from version 1
    private val createSqlV2 = """CREATE TABLE logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                glucose REAL,
                uric REAL,
                systolic INTEGER,
                diastolic INTEGER,
                remark TEXT NOT NULL
            )"""

    private val createMedSql = """CREATE TABLE medications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL,
                frequency TEXT NOT NULL
            )"""

    private val createDocSql = """CREATE TABLE documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                tag TEXT,
                ts INTEGER NOT NULL,
                note TEXT NOT NULL,
                file TEXT,
                mime TEXT
            )"""

    private val createInsSql = """CREATE TABLE insurances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                provider TEXT NOT NULL,
                policy TEXT NOT NULL,
                notes TEXT NOT NULL,
                ecard TEXT,
                ecard_mime TEXT
            )"""

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createSql)
        db.execSQL(createMedSql)
        db.execSQL(createDocSql)
        db.execSQL(createInsSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE logs RENAME TO logs_old")
            db.execSQL(createSqlV2)
            db.execSQL(
                """INSERT INTO logs (id, ts, glucose, uric, systolic, diastolic, remark)
                   SELECT id, ts, glucose, uric, systolic, diastolic, remark FROM logs_old"""
            )
            db.execSQL("DROP TABLE logs_old")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE logs ADD COLUMN tag TEXT")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE logs ADD COLUMN weight REAL")
            db.execSQL(createMedSql)
        }
        if (oldVersion < 5) {
            db.execSQL(createDocSql)
            db.execSQL(createInsSql)
        }
    }

    // ---------- Logs ----------
    private fun values(e: LogEntry) = ContentValues().apply {
        put("ts", e.timestamp)
        if (e.glucose != null) put("glucose", e.glucose) else putNull("glucose")
        if (e.uricAcid != null) put("uric", e.uricAcid) else putNull("uric")
        if (e.systolic != null) put("systolic", e.systolic) else putNull("systolic")
        if (e.diastolic != null) put("diastolic", e.diastolic) else putNull("diastolic")
        put("remark", e.remark)
        if (e.tag != null) put("tag", e.tag) else putNull("tag")
        if (e.weight != null) put("weight", e.weight) else putNull("weight")
    }

    fun insert(e: LogEntry) {
        writableDatabase.insert("logs", null, values(e))
    }

    fun insertMany(list: List<LogEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            list.forEach { db.insert("logs", null, values(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun update(e: LogEntry) {
        writableDatabase.update("logs", values(e), "id = ?", arrayOf(e.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("logs", "id = ?", arrayOf(id.toString()))
    }

    fun deleteLogsWithRemark(remark: String): Int =
        writableDatabase.delete("logs", "remark = ?", arrayOf(remark))

    fun getAll(): List<LogEntry> {
        val list = mutableListOf<LogEntry>()
        readableDatabase.query("logs", null, null, null, null, null, "ts DESC").use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iTs = c.getColumnIndexOrThrow("ts")
            val iGlu = c.getColumnIndexOrThrow("glucose")
            val iUric = c.getColumnIndexOrThrow("uric")
            val iSys = c.getColumnIndexOrThrow("systolic")
            val iDia = c.getColumnIndexOrThrow("diastolic")
            val iRem = c.getColumnIndexOrThrow("remark")
            val iTag = c.getColumnIndexOrThrow("tag")
            val iWt = c.getColumnIndexOrThrow("weight")
            while (c.moveToNext()) {
                list.add(
                    LogEntry(
                        id = c.getLong(iId),
                        timestamp = c.getLong(iTs),
                        glucose = if (c.isNull(iGlu)) null else c.getDouble(iGlu),
                        uricAcid = if (c.isNull(iUric)) null else c.getDouble(iUric),
                        systolic = if (c.isNull(iSys)) null else c.getInt(iSys),
                        diastolic = if (c.isNull(iDia)) null else c.getInt(iDia),
                        remark = c.getString(iRem),
                        tag = if (c.isNull(iTag)) null else c.getString(iTag),
                        weight = if (c.isNull(iWt)) null else c.getDouble(iWt)
                    )
                )
            }
        }
        return list
    }

    // ---------- Medications ----------
    private fun medValues(m: Medication) = ContentValues().apply {
        put("name", m.name)
        put("dosage", m.dosage)
        put("frequency", m.frequency)
    }

    fun insertMedication(m: Medication) {
        writableDatabase.insert("medications", null, medValues(m))
    }

    fun updateMedication(m: Medication) {
        writableDatabase.update("medications", medValues(m), "id = ?", arrayOf(m.id.toString()))
    }

    fun deleteMedication(id: Long) {
        writableDatabase.delete("medications", "id = ?", arrayOf(id.toString()))
    }

    fun getMedications(): List<Medication> {
        val list = mutableListOf<Medication>()
        readableDatabase.query(
            "medications", null, null, null, null, null, "name COLLATE NOCASE ASC"
        ).use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iName = c.getColumnIndexOrThrow("name")
            val iDose = c.getColumnIndexOrThrow("dosage")
            val iFreq = c.getColumnIndexOrThrow("frequency")
            while (c.moveToNext()) {
                list.add(Medication(c.getLong(iId), c.getString(iName), c.getString(iDose), c.getString(iFreq)))
            }
        }
        return list
    }

    // ---------- Medical records ----------
    private fun docValues(d: Document) = ContentValues().apply {
        put("name", d.name)
        if (d.tag != null) put("tag", d.tag) else putNull("tag")
        put("ts", d.dateMs)
        put("note", d.note)
        if (d.file != null) put("file", d.file) else putNull("file")
        if (d.mime != null) put("mime", d.mime) else putNull("mime")
    }

    fun insertDocument(d: Document): Long = writableDatabase.insert("documents", null, docValues(d))

    fun updateDocument(d: Document) {
        writableDatabase.update("documents", docValues(d), "id = ?", arrayOf(d.id.toString()))
    }

    fun deleteDocument(id: Long) {
        writableDatabase.delete("documents", "id = ?", arrayOf(id.toString()))
    }

    fun getDocuments(): List<Document> {
        val list = mutableListOf<Document>()
        readableDatabase.query("documents", null, null, null, null, null, "ts DESC, id DESC").use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iName = c.getColumnIndexOrThrow("name")
            val iTag = c.getColumnIndexOrThrow("tag")
            val iTs = c.getColumnIndexOrThrow("ts")
            val iNote = c.getColumnIndexOrThrow("note")
            val iFile = c.getColumnIndexOrThrow("file")
            val iMime = c.getColumnIndexOrThrow("mime")
            while (c.moveToNext()) {
                list.add(
                    Document(
                        id = c.getLong(iId),
                        name = c.getString(iName),
                        tag = if (c.isNull(iTag)) null else c.getString(iTag),
                        dateMs = c.getLong(iTs),
                        note = c.getString(iNote),
                        file = if (c.isNull(iFile)) null else c.getString(iFile),
                        mime = if (c.isNull(iMime)) null else c.getString(iMime)
                    )
                )
            }
        }
        return list
    }

    // ---------- Insurance ----------
    private fun insValues(i: Insurance) = ContentValues().apply {
        put("provider", i.provider)
        put("policy", i.policyNumber)
        put("notes", i.notes)
        if (i.ecard != null) put("ecard", i.ecard) else putNull("ecard")
        if (i.ecardMime != null) put("ecard_mime", i.ecardMime) else putNull("ecard_mime")
    }

    fun insertInsurance(i: Insurance): Long = writableDatabase.insert("insurances", null, insValues(i))

    fun updateInsurance(i: Insurance) {
        writableDatabase.update("insurances", insValues(i), "id = ?", arrayOf(i.id.toString()))
    }

    fun deleteInsurance(id: Long) {
        writableDatabase.delete("insurances", "id = ?", arrayOf(id.toString()))
    }

    fun getInsurances(): List<Insurance> {
        val list = mutableListOf<Insurance>()
        readableDatabase.query("insurances", null, null, null, null, null, "provider COLLATE NOCASE ASC").use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iProv = c.getColumnIndexOrThrow("provider")
            val iPol = c.getColumnIndexOrThrow("policy")
            val iNotes = c.getColumnIndexOrThrow("notes")
            val iCard = c.getColumnIndexOrThrow("ecard")
            val iMime = c.getColumnIndexOrThrow("ecard_mime")
            while (c.moveToNext()) {
                list.add(
                    Insurance(
                        id = c.getLong(iId),
                        provider = c.getString(iProv),
                        policyNumber = c.getString(iPol),
                        notes = c.getString(iNotes),
                        ecard = if (c.isNull(iCard)) null else c.getString(iCard),
                        ecardMime = if (c.isNull(iMime)) null else c.getString(iMime)
                    )
                )
            }
        }
        return list
    }
}