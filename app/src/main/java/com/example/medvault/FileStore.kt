package com.example.medvault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object FileStore {
    data class Imported(val storedName: String, val mime: String, val displayName: String)

    private fun dir(c: Context, sub: String): File = File(c.filesDir, sub).apply { mkdirs() }

    fun file(c: Context, sub: String, name: String): File = File(dir(c, sub), name)

    fun delete(c: Context, sub: String, name: String?) {
        if (name != null) file(c, sub, name).delete()
    }

    /** Copies a picked file into the app's private storage. */
    fun importFile(c: Context, uri: Uri, sub: String): Imported? {
        return try {
            val resolver = c.contentResolver
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            var display = "file"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                if (cur.moveToFirst()) display = cur.getString(0) ?: "file"
            }
            val ext = when {
                mime == "application/pdf" -> "pdf"
                mime.startsWith("image/") -> mime.substringAfter('/').let { if (it == "jpeg") "jpg" else it }
                else -> display.substringAfterLast('.', "bin")
            }
            val stored = "${UUID.randomUUID()}.$ext"
            val input = resolver.openInputStream(uri) ?: return null
            input.use { ins -> file(c, sub, stored).outputStream().use { ins.copyTo(it) } }
            Imported(stored, mime, display)
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Images ----------
    fun decodeImage(file: File, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) return null
        var s = 1
        while (bounds.outWidth / (s * 2) >= maxPx && bounds.outHeight / (s * 2) >= maxPx) s *= 2
        val bmp = BitmapFactory.decodeFile(
            file.absolutePath, BitmapFactory.Options().apply { inSampleSize = s }
        ) ?: return null
        val degrees = try {
            when (
                ExifInterface(file.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
        if (degrees == 0f) return bmp
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    // ---------- PDF ----------
    fun pdfPageCount(file: File): Int = try {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val r = PdfRenderer(pfd)
            try { r.pageCount } finally { r.close() }
        } finally {
            pfd.close()
        }
    } catch (e: Exception) {
        0
    }

    fun renderPdfPage(file: File, index: Int, targetWidth: Int): Bitmap? = try {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val r = PdfRenderer(pfd)
            try {
                if (index >= r.pageCount) {
                    null
                } else {
                    val page = r.openPage(index)
                    try {
                        val scale = targetWidth.toFloat() / page.width
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(targetWidth, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    } finally {
                        page.close()
                    }
                }
            } finally {
                r.close()
            }
        } finally {
            pfd.close()
        }
    } catch (e: Exception) {
        null
    }

    suspend fun thumbnail(file: File, mime: String?, maxPx: Int): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) null
                else if (mime == "application/pdf") renderPdfPage(file, 0, maxPx)?.asImageBitmap()
                else decodeImage(file, maxPx)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

    // ---------- Generated placeholders for sample data ----------
    private fun saveJpeg(c: Context, sub: String, bmp: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        file(c, sub, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun makeSampleDocument(c: Context, sub: String, title: String, subtitle: String, accent: Int): String {
        val w = 800
        val h = 1100
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = accent
        cv.drawRect(0f, 0f, w.toFloat(), 170f, p)
        p.color = Color.WHITE
        p.textSize = 44f
        p.isFakeBoldText = true
        cv.drawText(title.take(30), 40f, 100f, p)
        p.isFakeBoldText = false
        p.textSize = 28f
        cv.drawText(subtitle.take(48), 40f, 145f, p)
        p.color = Color.parseColor("#D9DEDD")
        var y = 240f
        var i = 0
        while (y < h - 140) {
            val len = if (i % 4 == 3) 380f else 620f + (i * 37 % 120)
            cv.drawRoundRect(40f, y, 40f + len, y + 22f, 11f, 11f, p)
            y += 52f
            i++
        }
        p.color = Color.parseColor("#9AA5A3")
        p.textSize = 30f
        cv.drawText("SAMPLE DOCUMENT", 40f, h - 60f, p)
        return saveJpeg(c, sub, bmp)
    }

    fun makeSampleCard(c: Context, sub: String, provider: String, policy: String, accent: Int): String {
        val w = 1012
        val h = 638
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = accent
        cv.drawRoundRect(20f, 20f, w - 20f, h - 20f, 40f, 40f, p)
        p.color = Color.WHITE
        p.textSize = 56f
        p.isFakeBoldText = true
        cv.drawText(provider.take(24), 70f, 150f, p)
        p.isFakeBoldText = false
        p.textSize = 34f
        cv.drawText("POLICY NUMBER", 70f, 380f, p)
        p.textSize = 60f
        p.isFakeBoldText = true
        cv.drawText(policy.take(20), 70f, 450f, p)
        p.isFakeBoldText = false
        p.textSize = 28f
        cv.drawText("SAMPLE E-CARD", 70f, h - 70f, p)
        return saveJpeg(c, sub, bmp)
    }
}