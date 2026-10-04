package com.example.medvault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

data class Profile(
    val name: String = "",
    val gender: String = "",
    val heightCm: Double? = null,
    val weightOverride: Double? = null,
    val useOverride: Boolean = false
)

object ProfileStore {
    private fun prefs(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private fun photoFile(c: Context) = File(c.filesDir, "profile.jpg")

    fun load(c: Context): Profile {
        val p = prefs(c)
        return Profile(
            name = p.getString("profile_name", "") ?: "",
            gender = p.getString("profile_gender", "") ?: "",
            heightCm = p.getString("profile_height", null)?.toDoubleOrNull(),
            weightOverride = p.getString("profile_weight_override", null)?.toDoubleOrNull(),
            useOverride = p.getBoolean("profile_use_override", false)
        )
    }

    fun save(c: Context, profile: Profile) {
        val e = prefs(c).edit()
        e.putString("profile_name", profile.name)
        e.putString("profile_gender", profile.gender)
        if (profile.heightCm != null) e.putString("profile_height", profile.heightCm.toString())
        else e.remove("profile_height")
        if (profile.weightOverride != null) e.putString("profile_weight_override", profile.weightOverride.toString())
        else e.remove("profile_weight_override")
        e.putBoolean("profile_use_override", profile.useOverride)
        e.apply()
    }

    fun loadPhoto(c: Context): ImageBitmap? {
        val f = photoFile(c)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
    }

    fun removePhoto(c: Context) {
        photoFile(c).delete()
    }

    /** Crops the picked image to a square, scales it to 512 px and stores it privately. */
    fun savePhoto(c: Context, uri: Uri): Boolean {
        return try {
            val resolver = c.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val side = minOf(bmp.width, bmp.height)
            val matrix = Matrix().apply { postRotate(degrees) }
            val square = Bitmap.createBitmap(
                bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side, matrix, true
            )
            val out = Bitmap.createScaledBitmap(square, 512, 512, true)
            photoFile(c).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (e: Exception) {
            false
        }
    }
}