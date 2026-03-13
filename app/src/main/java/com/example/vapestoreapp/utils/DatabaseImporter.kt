package com.example.vapestoreapp.utils

import android.content.Context
import android.net.Uri
import com.example.vapestoreapp.data.Repository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

/**
 * Импорт данных из JSON файла в Room БД.
 * Используется после обновления приложения для восстановления данных.
 */
object DatabaseImporter {

    /**
     * Импортирует данные из JSON файла в БД.
     * Очищает текущие данные и вставляет импортированные.
     * @return true при успехе, false при ошибке
     */
    suspend fun importDatabase(context: Context, backupFile: File, repository: Repository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = FileReader(backupFile).use { it.readText() }
                importFromJson(json, repository)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Импортирует данные из Uri (например, из файлового менеджера).
     */
    suspend fun importDatabase(context: Context, uri: Uri, repository: Repository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                    ?: return@withContext false
                importFromJson(json, repository)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    private suspend fun importFromJson(json: String, repository: Repository): Boolean {
        val gson = Gson()
        val type = object : TypeToken<DatabaseExporter.DatabaseBackup>() {}.type
        val backup = gson.fromJson<DatabaseExporter.DatabaseBackup>(json, type)
            ?: return false
        repository.importBackupData(
            products = backup.products,
            sales = backup.sales,
            cards = backup.paymentCards ?: emptyList()
        )
        return true
    }
}
