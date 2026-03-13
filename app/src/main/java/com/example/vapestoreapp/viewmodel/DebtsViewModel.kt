package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Debt
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.utils.displayName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DebtsViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _operationResult = MutableStateFlow("")
    val operationResult: StateFlow<String> = _operationResult

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private var debtsJob: Job? = null
    private var productsJob: Job? = null
    private var refreshJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        if (debtsJob == null) {
            debtsJob = viewModelScope.launch {
                repository.getAllActiveDebts().collect { _debts.value = it }
            }
        }
        if (productsJob == null) {
            productsJob = viewModelScope.launch {
                repository.getAllProducts().collect { _allProducts.value = it }
            }
        }
    }

    fun showCreateDebtDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDebtDialog() {
        _showCreateDialog.value = false
    }

    fun createDebt(customerName: String, productQuantities: List<Pair<Product, Int>>, totalAmount: Double) {
        if (customerName.isBlank()) {
            _operationResult.value = "❌ Укажите имя клиента"
            return
        }
        if (productQuantities.isEmpty()) {
            _operationResult.value = "❌ Добавьте товары"
            return
        }

        viewModelScope.launch {
            try {
                // Списываем склад сразу (товар отдан клиенту)
                productQuantities.forEach { (product, qty) ->
                    val current = repository.getProductById(product.id)
                    if (current == null) {
                        _operationResult.value = "❌ Товар #${product.id} не найден"
                        return@launch
                    }
                    if (qty <= 0) {
                        _operationResult.value = "❌ Некорректное количество"
                        return@launch
                    }
                    if (current.stock < qty) {
                        _operationResult.value = "❌ Недостаточно товара на складе для ${current.brand}"
                        return@launch
                    }
                    repository.updateProduct(current.copy(stock = current.stock - qty))
                }

                val items = productQuantities.map { (product, qty) ->
                    mapOf("productId" to product.id, "quantity" to qty)
                }
                val productsJson = Gson().toJson(items)

                val debt = Debt(
                    customerName = customerName.trim(),
                    products = productsJson,
                    totalAmount = totalAmount,
                    stockDeducted = true
                )
                repository.insertDebt(debt)
                _operationResult.value = "✅ Долг создан"
                _showCreateDialog.value = false
            } catch (e: Exception) {
                _operationResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun payDebt(debtId: Int) {
        viewModelScope.launch {
            try {
                val debt = repository.getDebtById(debtId)
                if (debt == null) {
                    _operationResult.value = "❌ Долг не найден"
                    return@launch
                }
                if (debt.isPaid) {
                    _operationResult.value = "⚠️ Долг уже погашен"
                    return@launch
                }
                val success = repository.payDebt(debtId)
                if (success) {
                    _operationResult.value = "✅ Долг погашен"
                } else {
                    _operationResult.value = "❌ Ошибка при погашении. Проверьте наличие товара на складе."
                }
            } catch (e: Exception) {
                _operationResult.value = "❌ Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                e.printStackTrace()
            }
        }
    }

    fun clearResult() {
        _operationResult.value = ""
    }

    fun formatDebtProducts(debt: Debt): String {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val items: List<Map<String, Any>> = Gson().fromJson(debt.products, type)
            items.joinToString(", ") { item ->
                val productId = (item["productId"] as? Double)?.toInt() ?: (item["productId"] as? Int) ?: 0
                val qty = (item["quantity"] as? Double)?.toInt() ?: (item["quantity"] as? Int) ?: 0
                val product = _allProducts.value.find { it.id == productId }
                if (product != null) "${product.displayName()} x$qty" else "Товар #$productId x$qty"
            }
        } catch (e: Exception) {
            debt.products
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            debtsJob?.cancelAndJoin()
            productsJob?.cancelAndJoin()
            debtsJob = null
            productsJob = null
            loadData()
        }
    }
}
