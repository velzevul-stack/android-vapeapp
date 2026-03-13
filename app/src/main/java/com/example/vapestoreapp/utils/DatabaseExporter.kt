package com.example.vapestoreapp.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.vapestoreapp.data.PaymentCard
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.data.Sale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экспорт данных из Room БД в JSON файл.
 * Критически важно для сохранения данных при обновлении приложения
 * (когда нет доступа к оригинальному keystore).
 */
object DatabaseExporter {

    data class ExportResult(
        val fileName: String,
        val downloadsUri: android.net.Uri? = null,
        val legacyFile: File? = null
    )

    data class DatabaseBackup(
        val version: Int,
        val exportDate: Long,
        val products: List<Product>,
        val sales: List<Sale>,
        val paymentCards: List<PaymentCard>? = null
    )

    /**
     * Экспортирует все данные из БД в JSON файл.
     * Сохраняет в Downloads/VapeStoreBackup_YYYYMMDD_HHmmss.json
     * @return ExportResult при успехе, null при ошибке
     */
    suspend fun exportDatabase(context: Context, repository: Repository): ExportResult? = withContext(Dispatchers.IO) {
        try {
            val products = repository.getAllProducts().first()
            val sales = repository.getAllSales().first()
            val cards = repository.getAllPaymentCards().first()

            val backup = DatabaseBackup(
                version = 3,
                exportDate = System.currentTimeMillis(),
                products = products,
                sales = sales.filter { !it.isCancelled },
                paymentCards = cards
            )

            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(backup)

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "VapeStoreBackup_${dateFormat.format(Date())}.json"

            // Android 10+ (scoped storage): сохраняем прямо в Downloads через MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    
                    // Пробуем сохранить в подпапку Downloads/VapeStoreBackups
                    var uri: android.net.Uri? = null
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        // MediaStore требует прямые слеши, не File.separator
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/VapeStoreBackups"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    
                    uri = resolver.insert(collection, values)
                    
                    // Если не удалось создать в подпапке, пробуем сохранить прямо в Downloads
                    if (uri == null) {
                        val fallbackValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        uri = resolver.insert(collection, fallbackValues)
                    }

                    if (uri != null) {
                        try {
                            resolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray(Charsets.UTF_8))
                                out.flush()
                            } ?: run {
                                resolver.delete(uri, null, null)
                                throw Exception("Не удалось открыть поток для записи")
                            }

                            // Завершаем запись
                            val updateValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_PENDING, 0)
                            }
                            resolver.update(uri, updateValues, null, null)

                            return@withContext ExportResult(fileName = fileName, downloadsUri = uri)
                        } catch (e: Exception) {
                            try {
                                resolver.delete(uri, null, null)
                            } catch (deleteException: Exception) {
                                // Игнорируем ошибку удаления
                            }
                            e.printStackTrace()
                            // Пробуем fallback на legacy метод
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Пробуем fallback на legacy метод
                }
            }

            // Legacy fallback: app-specific external files
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val backupDir = File(dir, "VapeStoreBackups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val file = File(backupDir, fileName)
            FileWriter(file).use { it.write(json) }
            ExportResult(fileName = fileName, legacyFile = file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
