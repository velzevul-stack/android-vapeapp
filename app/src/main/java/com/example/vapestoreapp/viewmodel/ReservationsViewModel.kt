package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Reservation
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.utils.displayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReservationsViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _operationResult = MutableStateFlow("")
    val operationResult: StateFlow<String> = _operationResult

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private var reservationsJob: Job? = null
    private var productsJob: Job? = null
    private var refreshJob: Job? = null

    init {
        startCollectors()
        // Автоматически помечаем истекшие резервы (чтобы не копились в БД)
        viewModelScope.launch {
            try {
                val returnedCount = repository.returnExpiredReservations()
                if (returnedCount > 0) {
                    _operationResult.value = "✅ Автоматически возвращено $returnedCount истекших резервов"
                }
            } catch (e: Exception) {
                // Не ломаем экран из‑за фоновой очистки
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                restartCollectors()
                val returnedCount = repository.returnExpiredReservations()
                if (returnedCount > 0) {
                    _operationResult.value = "✅ Возвращено $returnedCount истекших резервов"
                } else {
                    _operationResult.value = "✅ Резервы обновлены"
                }
            } catch (e: Exception) {
                _operationResult.value = "❌ Ошибка обновления: ${e.message}"
            }
        }
    }

    private fun startCollectors() {
        if (reservationsJob == null) {
            reservationsJob = viewModelScope.launch {
                repository.getAllActiveReservations().collect { _reservations.value = it }
            }
        }
        if (productsJob == null) {
            productsJob = viewModelScope.launch {
                repository.getAllProducts().collect { _allProducts.value = it }
            }
        }
    }

    private suspend fun restartCollectors() {
        stopCollectors()
        startCollectors()
    }

    private suspend fun stopCollectors() {
        reservationsJob?.cancelAndJoin()
        productsJob?.cancelAndJoin()
        reservationsJob = null
        productsJob = null
    }

    fun showCreateReservationDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateReservationDialog() {
        _showCreateDialog.value = false
    }

    fun createReservation(customerName: String, productId: Int, quantity: Int, expirationDateTimeMillis: Long) {
        if (customerName.isBlank()) {
            _operationResult.value = "❌ Укажите имя клиента"
            return
        }
        if (quantity <= 0) {
            _operationResult.value = "❌ Укажите количество"
            return
        }
        if (expirationDateTimeMillis <= System.currentTimeMillis()) {
            _operationResult.value = "❌ Дата и время резерва должны быть в будущем"
            return
        }

        viewModelScope.launch {
            try {
                val product = repository.getProductById(productId)
                if (product == null) {
                    _operationResult.value = "❌ Товар не найден"
                    return@launch
                }
                val reservedForProduct = repository.getReservationsForProduct(productId).sumOf { it.quantity }
                if (product.stock - reservedForProduct < quantity) {
                    _operationResult.value = "❌ Недостаточно товара на складе"
                    return@launch
                }

                val reservation = Reservation(
                    customerName = customerName.trim(),
                    productId = productId,
                    quantity = quantity,
                    expirationDate = expirationDateTimeMillis
                )
                repository.insertReservation(reservation)
                _operationResult.value = "✅ Резерв создан"
                _showCreateDialog.value = false
            } catch (e: Exception) {
                _operationResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun createReservation(customerName: String, productId: Int, quantity: Int, expirationDays: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, expirationDays)
        createReservation(
            customerName = customerName,
            productId = productId,
            quantity = quantity,
            expirationDateTimeMillis = calendar.timeInMillis
        )
    }

    fun sellReservation(reservationId: Int) {
        viewModelScope.launch {
            try {
                val success = repository.sellReservation(reservationId)
                _operationResult.value = if (success) "✅ Резерв продан" else "❌ Ошибка при продаже"
            } catch (e: Exception) {
                _operationResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun cancelReservation(reservationId: Int) {
        viewModelScope.launch {
            val reservation = repository.getReservationById(reservationId)
            if (reservation != null) {
                repository.updateReservation(reservation.copy(isSold = true))
                _operationResult.value = "✅ Резерв отменён"
            }
        }
    }

    fun clearResult() {
        _operationResult.value = ""
    }

    fun getProductName(productId: Int): String {
        val product = _allProducts.value.find { it.id == productId }
        return product?.displayName() ?: "Товар #$productId"
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun isExpired(reservation: Reservation): Boolean {
        return System.currentTimeMillis() > reservation.expirationDate
    }
}
