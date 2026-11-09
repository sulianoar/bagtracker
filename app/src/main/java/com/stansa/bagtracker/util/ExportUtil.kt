package com.stansa.bagtracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.stansa.bagtracker.data.ItemWithTags
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportUtil {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.FRANCE)

    data class ExportData(
        val exportedAt: String,
        val version: Int = 2,
        val items: List<ItemWithTags>
    )

    fun exportToJson(context: Context, items: List<ItemWithTags>): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "bagtracker_${dateFormat.format(Date())}.json")
        FileWriter(file).use { it.write(gson.toJson(ExportData(Date().toString(), items = items))) }
        return file
    }

    fun shareExportFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BagTracker - Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exporter via..."))
    }

    fun parseImportFile(context: Context, uri: Uri): ExportData? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
            gson.fromJson(json, ExportData::class.java)
        } catch (e: Exception) { null }
    }

    fun cleanOldExports(context: Context) {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) return
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    }
}