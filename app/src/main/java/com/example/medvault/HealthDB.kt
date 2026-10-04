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

class HealthDb(context: Context) :
    SQLiteOpenHelper(context, "health.db", null, 4) {

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

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createSql)
        db.execSQL(createMedSql)
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
}