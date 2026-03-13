package com.example.vapestoreapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar

object ExpirationChecker {

    private const val TAG = "ExpirationChecker"
    private const val EXPIRATION_DATE = "2026-01-15"

    // Разные сервера для проверки
    private val checkUrls = listOf(
        "https://raw.githubusercontent.com/example/vapestore-app/main/expiration.json",
        "https://api.jsonbin.io/v3/b/your-bin-id",
        "https://api.jsonserve.com/your-endpoint",
        "http://10.0.2.2:8080/check" // Для эмулятора Android
    )

    suspend fun checkExpiration(context: Context): CheckResult {
        return try {
            // Пробуем онлайн проверку
            val onlineResult = checkOnline()
            if (onlineResult.isExpired) {
                return onlineResult
            }

            // Если онлайн проверка не удалась, проверяем локально
            checkLocal()

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки: ${e.message}")
            checkLocal()
        }
    }

    private suspend fun checkOnline(): CheckResult {
        for (url in checkUrls) {
            try {
                val response = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }

                Log.d(TAG, "Ответ от $url: $response")

                // Простой парсинг JSON
                return parseResponse(response)

            } catch (e: Exception) {
                Log.w(TAG, "Не удалось подключиться к $url: ${e.message}")
                continue
            }
        }

        return CheckResult(
            isExpired = false,
            message = "Онлайн проверка недоступна",
            daysLeft = getDaysUntilExpiration()
        )
    }

    private fun parseResponse(response: String): CheckResult {
        return try {
            // Простой парсинг JSON (для сложных JSON лучше использовать библиотеку)
            when {
                response.contains("\"expired\":true") ||
                        response.contains("\"status\":\"expired\"") -> {
                    CheckResult(
                        isExpired = true,
                        message = "Приложение заблокировано сервером",
                        daysLeft = 0
                    )
                }

                response.contains("\"expiration_date\":") -> {
                    val regex = "\"expiration_date\":\"(\\d{4}-\\d{2}-\\d{2})\"".toRegex()
                    val match = regex.find(response)
                    val serverDate = match?.groupValues?.get(1) ?: EXPIRATION_DATE

                    CheckResult(
                        isExpired = isDateExpired(serverDate),
                        message = "Серверная дата: $serverDate",
                        daysLeft = getDaysUntilDate(serverDate)
                    )
                }

                else -> {
                    checkLocal()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга ответа: ${e.message}")
            checkLocal()
        }
    }

    private fun checkLocal(): CheckResult {
        val isExpired = isDateExpired(EXPIRATION_DATE)
        val daysLeft = getDaysUntilExpiration()

        return CheckResult(
            isExpired = isExpired,
            message = if (isExpired) "Локальная проверка: срок истёк" else "Локальная проверка: OK",
            daysLeft = daysLeft
        )
    }

    private fun isDateExpired(dateString: String): Boolean {
        return try {
            val parts = dateString.split("-")
            if (parts.size != 3) return false

            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            val expirationCalendar = Calendar.getInstance().apply {
                set(year, month, day, 23, 59, 59)
            }

            val currentCalendar = Calendar.getInstance()
            currentCalendar.after(expirationCalendar)

        } catch (e: Exception) {
            false
        }
    }

    private fun getDaysUntilExpiration(): Int {
        return getDaysUntilDate(EXPIRATION_DATE)
    }

    private fun getDaysUntilDate(dateString: String): Int {
        return try {
            val parts = dateString.split("-")
            if (parts.size != 3) return -1

            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            val expirationCalendar = Calendar.getInstance().apply {
                set(year, month, day, 23, 59, 59)
            }

            val currentCalendar = Calendar.getInstance()
            val diffMillis = expirationCalendar.timeInMillis - currentCalendar.timeInMillis
            val days = diffMillis / (1000 * 60 * 60 * 24)

            days.toInt().coerceAtLeast(0)

        } catch (e: Exception) {
            -1
        }
    }

    data class CheckResult(
        val isExpired: Boolean,
        val message: String,
        val daysLeft: Int
    )
}