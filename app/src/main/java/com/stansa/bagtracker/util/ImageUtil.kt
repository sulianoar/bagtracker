package com.stansa.bagtracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtil {

    fun saveImageFromUri(context: Context, uri: Uri, itemId: Long): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val dir = File(context.filesDir, "item_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "item_$itemId.jpg")
            val out = FileOutputStream(file)
            resizeBitmap(bitmap, 1024).compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deletePhoto(path: String) {
        try { File(path).delete() } catch (e: Exception) {}
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxSize && h <= maxSize) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        return if (w > h) Bitmap.createScaledBitmap(bitmap, maxSize, (maxSize / ratio).toInt(), true)
        else Bitmap.createScaledBitmap(bitmap, (maxSize * ratio).toInt(), maxSize, true)
    }
}