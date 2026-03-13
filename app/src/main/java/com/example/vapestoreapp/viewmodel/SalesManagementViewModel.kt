package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.data.Sale
import com.example.vapestoreapp.utils.displayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SalesManagementViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)

    // Состояния UI
    private val _activeSales = MutableStateFlow<List<Sale>>(emptyList())
    val activeSales: StateFlow<List<Sale>> = _activeSales

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _editingSale = MutableStateFlow<Sale?>(null)
    val editingSale: StateFlow<Sale?> = _editingSale

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog

    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm

    private val _operationResult = MutableStateFlow("")
    val operationResult: StateFlow<String> = _operationResult

    // Данные для редактирования
    private val _editQuantity = MutableStateFlow(1)
    val editQuantity: StateFlow<Int> = _editQuantity

    private val _editDiscount = MutableStateFlow(0.0)
    val editDiscount: StateFlow<Double> = _editDiscount

    private val _editPaymentMethod = MutableStateFlow("cash")
    val editPaymentMethod: StateFlow<String> = _editPaymentMethod

    private val _editComment = MutableStateFlow("")
    val editComment: StateFlow<String> = _editComment

    private val _editDate = MutableStateFlow(0L)
    val editDate: StateFlow<Long> = _editDate

    // Для отображения названий товаров
    private val _saleDetails = MutableStateFlow<Map<Int, String>>(emptyMap())
    val saleDetails: StateFlow<Map<Int, String>> = _saleDetails

    init {
        loadAllDataRealTime()
    }

    private var realtimeJob: Job? = null
    private var refreshJob: Job? = null

    private fun loadAllDataRealTime() {
        if (realtimeJob != null) return
        realtimeJob = viewModelScope.launch {
            // Объединяем два потока в реальном времени
            repository.getActiveSales()
                .combine(repository.getAllProducts()) { sales, products ->
                    Pair(sales, products)
                }
                .collect { (sales, products) ->
                    // Обрабатываем полученные данные
                    _activeSales.value = sales
                    _allProducts.value = products

                    // Формируем map с названиями товаров
                    val details = mutableMapOf<Int, String>()
                    sales.forEach { sale ->
                        val product = products.find { p -> p.id == sale.productId }
                        if (product != null) {
                            details[sale.id] = product.displayName()
                        } else {
                            details[sale.id] = "Товар #${sale.productId}"
                        }
                    }

                    _saleDetails.value = details
                }
        }
    }

    // Открыть редактирование продажи
    fun openEditSale(sale: Sale) {
        _editingSale.value = sale
        _editQuantity.value = sale.quantity
        _editDiscount.value = sale.discount
        _editPaymentMethod.value = sale.paymentMethod
        _editComment.value = sale.comment ?: ""
        _editDate.value = sale.date

        // Найти товар
        viewModelScope.launch {
            val product = repository.getProductById(sale.productId)
            _selectedProduct.value = product
        }

        _showEditDialog.value = true
    }

    // Выбрать товар при редактировании
    fun selectProductForEdit(product: Product) {
        _selectedProduct.value = product
    }

    // Обновить количество
    fun updateEditQuantity(quantity: Int) {
        _editQuantity.value = quantity.coerceAtLeast(1)
    }

    // Обновить скидку с валидацией
    fun updateEditDiscount(discount: Double) {
        val product = _selectedProduct.value
        if (product == null) {
            _editDiscount.value = discount.coerceAtLeast(0.0)
            return
        }
        val totalPrice = product.retailPrice * _editQuantity.value
        _editDiscount.value = discount.coerceIn(0.0, totalPrice)
    }
    
    // Получить максимально допустимую скидку для редактирования
    fun getMaxEditDiscount(): Double {
        val product = _selectedProduct.value ?: return 0.0
        return product.retailPrice * _editQuantity.value
    }

    // Обновить метод оплаты
    fun updatePaymentMethod(method: String) {
        _editPaymentMethod.value = method
    }

    // Обновить комментарий
    fun updateEditComment(comment: String) {
        _editComment.value = comment
    }

    fun updateEditDate(dateMillis: Long) {
        _editDate.value = dateMillis
    }

    // Сохранить исправление (ОБНОВЛЕНО ДЛЯ РЕАЛЬНОГО ВРЕМЕНИ)
    fun saveCorrection() {
        val sale = _editingSale.value ?: return
        val product = _selectedProduct.value ?: return

        // Валидация
        if (_editQuantity.value <= 0) {
            _operationResult.value = "❌ Ошибка: количество должно быть больше 0"
            return
        }
        
        val totalPrice = product.retailPrice * _editQuantity.value
        if (_editDiscount.value > totalPrice) {
            _operationResult.value = "❌ Ошибка: скидка не может быть больше стоимости товара (${"%.2f".format(totalPrice)} BYN)"
            return
        }
        
        if (_editDiscount.value < 0) {
            _operationResult.value = "❌ Ошибка: скидка не может быть отрицательной"
            return
        }
        
        if (product.retailPrice <= 0) {
            _operationResult.value = "❌ Ошибка: некорректная цена товара"
            return
        }

        viewModelScope.launch {
            val success = repository.correctSale(
                originalSaleId = sale.id,
                newProductId = product.id,
                newQuantity = _editQuantity.value,
                newDiscount = _editDiscount.value,
                newPaymentMethod = _editPaymentMethod.value,
                newDate = _editDate.value,
                comment = _editComment.value.ifEmpty { null }
            )

            _operationResult.value = if (success) {
                "✅ Продажа #${sale.id} успешно исправлена"
            } else {
                "❌ Ошибка при исправлении продажи"
            }

            if (success) {
                _showEditDialog.value = false
                _editingSale.value = null
                // НЕ нужно вызывать loadAllData - данные обновятся автоматически через Flow
            }
        }
    }

    // Удалить продажу (ОБНОВЛЕНО ДЛЯ РЕАЛЬНОГО ВРЕМЕНИ)
    fun deleteSale(saleId: Int) {
        viewModelScope.launch {
            val success = repository.deleteSale(saleId)
            _operationResult.value = if (success) {
                "✅ Продажа #$saleId удалена (товар возвращен на склад)"
            } else {
                "❌ Ошибка при удалении продажи"
            }

            if (success) {
                _showDeleteConfirm.value = false
                // НЕ нужно вызывать loadAllData - данные обновятся автоматически через Flow
            }
        }
    }

    // Открыть подтверждение удаления
    fun openDeleteConfirm(saleId: Int) {
        _editingSale.value = _activeSales.value.find { it.id == saleId }
        _showDeleteConfirm.value = true
    }

    // Закрыть диалоги
    fun closeEditDialog() {
        _showEditDialog.value = false
        _editingSale.value = null
    }

    fun closeDeleteConfirm() {
        _showDeleteConfirm.value = false
    }

    fun clearResult() {
        _operationResult.value = ""
    }

    fun loadAllData() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            realtimeJob?.cancelAndJoin()
            realtimeJob = null
            loadAllDataRealTime()
        }
    }
}