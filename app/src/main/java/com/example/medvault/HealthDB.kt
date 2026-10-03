package com.example.medvault

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HealthDb(context: Context) :
    SQLiteOpenHelper(context, "health.db", null, 2) {

    private val createSql = """CREATE TABLE logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                glucose REAL,
                uric REAL,
                systolic INTEGER,
                diastolic INTEGER,
                remark TEXT NOT NULL
            )"""

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE logs RENAME TO logs_old")
            db.execSQL(createSql)
            db.execSQL(
                """INSERT INTO logs (id, ts, glucose, uric, systolic, diastolic, remark)
                   SELECT id, ts, glucose, uric, systolic, diastolic, remark FROM logs_old"""
            )
            db.execSQL("DROP TABLE logs_old")
        }
    }

    private fun values(e: LogEntry) = ContentValues().apply {
        put("ts", e.timestamp)
        if (e.glucose != null) put("glucose", e.glucose) else putNull("glucose")
        if (e.uricAcid != null) put("uric", e.uricAcid) else putNull("uric")
        if (e.systolic != null) put("systolic", e.systolic) else putNull("systolic")
        if (e.diastolic != null) put("diastolic", e.diastolic) else putNull("diastolic")
        put("remark", e.remark)
    }

    fun insert(e: LogEntry) {
        writableDatabase.insert("logs", null, values(e))
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
            while (c.moveToNext()) {
                list.add(
                    LogEntry(
                        id = c.getLong(iId),
                        timestamp = c.getLong(iTs),
                        glucose = if (c.isNull(iGlu)) null else c.getDouble(iGlu),
                        uricAcid = if (c.isNull(iUric)) null else c.getDouble(iUric),
                        systolic = if (c.isNull(iSys)) null else c.getInt(iSys),
                        diastolic = if (c.isNull(iDia)) null else c.getInt(iDia),
                        remark = c.getString(iRem)
                    )
                )
            }
        }
        return list
    }
}