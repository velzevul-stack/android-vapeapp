package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.PaymentCard
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.data.Sale
import com.example.vapestoreapp.data.DebtProductItem
import com.example.vapestoreapp.utils.displayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SellViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)

    // Состояния UI
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount

    private val _comment = MutableStateFlow("")
    val comment: StateFlow<String> = _comment

    private val _saleResult = MutableStateFlow("")
    val saleResult: StateFlow<String> = _saleResult

    private val _showBrandsDialog = MutableStateFlow(false)
    val showBrandsDialog: StateFlow<Boolean> = _showBrandsDialog

    private val _showFlavorsDialog = MutableStateFlow(false)
    val showFlavorsDialog: StateFlow<Boolean> = _showFlavorsDialog

    // Данные из базы
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _reservedQuantities = MutableStateFlow<Map<Int, Int>>(emptyMap()) // productId -> reserved quantity
    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands

    private val _flavors = MutableStateFlow<List<Product>>(emptyList())
    val flavors: StateFlow<List<Product>> = _flavors

    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog: StateFlow<Boolean> = _showCategoryDialog

    private val _showBrandsDialogForCategory = MutableStateFlow(false)
    val showBrandsDialogForCategory: StateFlow<Boolean> = _showBrandsDialogForCategory

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _brandsForCategory = MutableStateFlow<List<String>>(emptyList())
    val brandsForCategory: StateFlow<List<String>> = _brandsForCategory

    init {
        loadAllProducts()
        loadBrandsWithStock()
        loadReservedQuantities()
        startExpiredReservationsCleanup()
        loadPaymentCards()
    }

    private var reservedJob: Job? = null
    private var expiredCleanupJob: Job? = null
    private var cardsJob: Job? = null

    private val _paymentCards = MutableStateFlow<List<PaymentCard>>(emptyList())
    val paymentCards: StateFlow<List<PaymentCard>> = _paymentCards

    private val _cardQuery = MutableStateFlow("")
    val cardQuery: StateFlow<String> = _cardQuery

    private val _selectedCardId = MutableStateFlow<Int?>(null)
    val selectedCardId: StateFlow<Int?> = _selectedCardId

    private fun loadPaymentCards() {
        if (cardsJob != null) return
        cardsJob = viewModelScope.launch {
            repository.getActivePaymentCards().collect { _paymentCards.value = it }
        }
    }

    fun updateCardQuery(value: String) {
        _cardQuery.value = value
        _selectedCardId.value = null
    }

    fun selectCard(card: PaymentCard) {
        _selectedCardId.value = card.id
        _cardQuery.value = card.label
    }

    fun addCard(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val card = repository.upsertPaymentCard(trimmed)
            selectCard(card)
        }
    }

    private fun startExpiredReservationsCleanup() {
        if (expiredCleanupJob != null) return
        expiredCleanupJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.returnExpiredReservations()
                } catch (_: Exception) {
                    // Игнорируем фоновые ошибки, чтобы не ломать UI
                }
                delay(60_000)
            }
        }
    }

    private fun loadAllProducts() {
        viewModelScope.launch {
            // ИСПРАВЛЕНО: загружаем все товары для поиска
            repository.getAllProducts().collect { products ->
                _allProducts.value = products
                println("✅ SellViewModel: Всего товаров: ${products.size}")
                println("✅ SellViewModel: Товаров с остатком: ${products.count { it.stock > 0 }}")
            }
        }
    }
    
    private fun loadReservedQuantities() {
        if (reservedJob != null) return
        reservedJob = viewModelScope.launch {
            repository.getReservedQuantityByProductFlow().collect { reserved ->
                _reservedQuantities.value = reserved
            }
        }
    }

    private fun loadBrandsWithStock() {
        viewModelScope.launch {
            // ИСПРАВЛЕНО: используем правильный метод
            repository.getBrandsWithStock().collect { brandsList ->
                println("✅ SellViewModel: Загружено брендов с остатком: ${brandsList.size}")
                _brands.value = brandsList
            }
        }
    }

    // Поиск товаров (по названию или штрих-коду)
    fun searchProducts(query: String): List<Product> {
        if (query.isEmpty()) return emptyList()

        val reservedQuantities = _reservedQuantities.value
        return _allProducts.value.filter { product ->
            (product.brand.contains(query, ignoreCase = true) ||
                    product.flavor.contains(query, ignoreCase = true) ||
                    product.specification.contains(query, ignoreCase = true) ||
                    product.barcode?.contains(query) == true) &&
                    hasAvailableStock(product, reservedQuantities)  // ТОЛЬКО товары с доступным остатком (с учетом резервов)!
        }
    }
    
    // Проверка доступного остатка с учетом активных резервов
    private fun hasAvailableStock(product: Product, reservedQuantities: Map<Int, Int>): Boolean {
        val reservedForProduct = reservedQuantities[product.id] ?: 0
        val availableStock = product.stock - reservedForProduct
        return availableStock > 0
    }

    // Методы для управления диалогами
    fun showCategoryDialog() {
        _showCategoryDialog.value = true
    }

    fun hideCategoryDialog() {
        _showCategoryDialog.value = false
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _showCategoryDialog.value = false

        // Загружаем бренды для этой категории
        viewModelScope.launch {
            repository.getBrandsByCategory(category).collect { brands ->
                // Фильтруем только те бренды, у которых есть товары в наличии
                val brandsWithStock = brands.filter { brand ->
                    // Проверяем, есть ли у этого бренда товары с остатком
                    val products = repository.getProductsByBrand(brand)
                    products.any { it.stock > 0 }
                }
                _brandsForCategory.value = brandsWithStock
            }
        }

        // Показываем диалог выбора бренда
        _showBrandsDialogForCategory.value = true
    }

    fun hideBrandsDialogForCategory() {
        _showBrandsDialogForCategory.value = false
        _selectedCategory.value = null
    }

    // Загрузить вкусы для выбранного бренда (только с остатком)
    fun loadFlavorsForBrand(brand: String) {
        viewModelScope.launch {
            _flavors.value = repository.getFlavorsByBrand(brand)
        }
    }

    // Выбрать товар
    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun loadProductsByBrandForCategory(brand: String) {
        viewModelScope.launch {
            val products = repository.getFlavorsByBrand(brand)
            _flavors.value = products
            _showBrandsDialogForCategory.value = false
            _showFlavorsDialog.value = true
        }
    }

    // Обновить скидку с валидацией
    fun updateDiscount(discount: Double) {
        val product = _selectedProduct.value
        if (product == null) {
            _discount.value = discount.coerceAtLeast(0.0)
            return
        }
        val totalPrice = product.retailPrice * _quantity.value
        _discount.value = discount.coerceIn(0.0, totalPrice)
    }
    
    // Получить максимально допустимую скидку
    fun getMaxDiscount(): Double {
        val product = _selectedProduct.value ?: return 0.0
        return product.retailPrice * _quantity.value
    }

    // Обновить комментарий
    fun updateComment(comment: String) {
        _comment.value = comment
    }

    // Продать товар
    fun sellProduct() {
        val product = _selectedProduct.value
        if (product == null || product.stock <= 0) {
            _saleResult.value = "❌ Ошибка: товар не выбран или нет в наличии"
            return
        }

        val quantityToSell = _quantity.value
        if (quantityToSell <= 0) {
            _saleResult.value = "❌ Ошибка: количество должно быть больше 0"
            return
        }
        
        // Проверяем доступность с учетом резервов и выполняем продажу
        viewModelScope.launch {
            val reservedForProduct = repository.getReservationsForProduct(product.id).sumOf { if (it.isSold) 0 else it.quantity }
            val availableStock = product.stock - reservedForProduct
            
            if (quantityToSell > availableStock) {
                _saleResult.value = "❌ Ошибка: недостаточно товара на складе (доступно: ${availableStock})"
                return@launch
            }

            // Валидация скидки
            val totalPrice = product.retailPrice * quantityToSell
            if (_discount.value > totalPrice) {
                _saleResult.value = "❌ Ошибка: скидка не может быть больше стоимости товара (${"%.2f".format(totalPrice)} BYN)"
                return@launch
            }
            
            if (_discount.value < 0) {
                _saleResult.value = "❌ Ошибка: скидка не может быть отрицательной"
                return@launch
            }

            // Проверка для долга и резерва
            if ((_saleType.value == "debt" || _saleType.value == "reservation") && _customerName.value.isBlank()) {
                _saleResult.value = "❌ Укажите имя клиента"
                return@launch
            }
            
            // Валидация цены товара
            if (product.retailPrice <= 0) {
                _saleResult.value = "❌ Ошибка: некорректная цена товара"
                return@launch
            }
            try {
                when (_saleType.value) {
                    "debt" -> {
                        // Создаем долг
                        val totalAmount = (product.retailPrice * quantityToSell) - _discount.value
                        val productsJson = com.google.gson.Gson().toJson(
                            listOf(DebtProductItem(product.id, quantityToSell))
                        )
                        val debt = com.example.vapestoreapp.data.Debt(
                            customerName = _customerName.value.trim(),
                            products = productsJson,
                            totalAmount = totalAmount,
                            date = System.currentTimeMillis(),
                            isPaid = false,
                            stockDeducted = true
                        )
                        repository.insertDebt(debt)

                        // Товар отдан клиенту — списываем со склада сразу
                        repository.updateProduct(product.copy(stock = product.stock - quantityToSell))
                        _saleResult.value = """
                            ✅ ДОЛГ СОЗДАН!
                            
                            Клиент: ${_customerName.value}
                            ${product.displayName()}
                            Количество: $quantityToSell шт.
                            Сумма: ${"%.2f".format(totalAmount)} BYN
                            
                            Долг можно погасить в разделе "Управление" → "Долги"
                        """.trimIndent()
                        val message = _saleResult.value
                        resetSaleCompletely()
                        _saleResult.value = message
                    }
                    "reservation" -> {
                        // Создаем резерв
                        val expirationDateTime = _expirationDate.value
                        if (expirationDateTime == null) {
                            _saleResult.value = "❌ Укажите дату и время окончания резерва"
                            return@launch
                        }
                        if (expirationDateTime <= System.currentTimeMillis()) {
                            _saleResult.value = "❌ Дата и время резерва должны быть в будущем"
                            return@launch
                        }
                        val reservedForProduct = repository.getReservationsForProduct(product.id).sumOf { it.quantity }
                        if (product.stock - reservedForProduct < quantityToSell) {
                            _saleResult.value = "❌ Недостаточно товара на складе (учитывая резервы)"
                            return@launch
                        }
                        val reservation = com.example.vapestoreapp.data.Reservation(
                            customerName = _customerName.value.trim(),
                            productId = product.id,
                            quantity = quantityToSell,
                            expirationDate = expirationDateTime,
                            isSold = false
                        )
                        repository.insertReservation(reservation)
                        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        _saleResult.value = """
                            ✅ РЕЗЕРВ СОЗДАН!
                            
                            Клиент: ${_customerName.value}
                            ${product.displayName()}
                            Количество: $quantityToSell шт.
                            До: ${dateFormat.format(java.util.Date(expirationDateTime))}
                            
                            Резерв можно продать в разделе "Управление" → "Резервы"
                        """.trimIndent()
                        val message = _saleResult.value
                        resetSaleCompletely()
                        _saleResult.value = message
                    }
                    else -> {
                        // Обычная продажа
                        val revenue = (product.retailPrice * quantityToSell) - _discount.value
                        val profit = ((product.retailPrice - product.purchasePrice) * quantityToSell) - _discount.value
                        val totalPrice = product.retailPrice * quantityToSell

                        val payment = paymentMethod.value
                        val cardId = _selectedCardId.value
                        val splitUi = splitPaymentUi.value
                        if (payment == "split" && !splitUi.isValid) {
                            _saleResult.value = "❌ Ошибка оплаты: ${splitUi.error ?: "проверьте суммы"}"
                            return@launch
                        }
                        if ((payment == "card" || payment == "split") && cardId == null) {
                            _saleResult.value = "❌ Выберите карту для безнала"
                            return@launch
                        }

                        // Уменьшаем количество на складе
                        repository.updateProduct(product.copy(stock = product.stock - quantityToSell))

                        // Создаем запись о продаже
                        val sale = Sale(
                            productId = product.id,
                            comment = _comment.value.ifEmpty { null },
                            discount = _discount.value,
                            revenue = revenue,
                            profit = profit,
                            quantity = quantityToSell,
                            paymentMethod = payment,
                            cashAmount = if (payment == "split") splitUi.cash else null,
                            cardAmount = if (payment == "split") splitUi.card else null,
                            cardId = if (payment == "card" || payment == "split") cardId else null
                        )
                        repository.insertSale(sale)

                        val paymentEmoji = when (payment) {
                            "cash" -> "💵"
                            "card" -> "💳"
                            else -> "💵+💳"
                        }
                        val paymentText = when (payment) {
                            "cash" -> "Наличные"
                            "card" -> "Карта"
                            else -> "Наличные + Карта"
                        }
                        val cardLabel = cardId?.let { id -> _paymentCards.value.find { it.id == id }?.label }

                        _saleResult.value = """
                            🎉 ТОВАР ПРОДАН!
                            
                            ${product.displayName()}
                            Количество: $quantityToSell шт.
                            
                            ──────────────────
                            Сумма: ${"%.2f".format(totalPrice)} BYN
                            Скидка: ${"%.2f".format(_discount.value)} BYN
                            Итого: ${"%.2f".format(revenue)} BYN
                            Прибыль: ${"%.2f".format(profit)} BYN
                            
                            Оплата: $paymentEmoji $paymentText
                            ${if (payment == "card" || payment == "split") "Карта: ${cardLabel ?: "—"}" else ""}
                            ${if (payment == "split") "  • Наличные: ${"%.2f".format(splitUi.cash ?: 0.0)} BYN\n  • Карта: ${"%.2f".format(splitUi.card ?: 0.0)} BYN" else ""}
                            ${if (_comment.value.isNotEmpty()) "Комментарий: ${_comment.value}" else ""}
                            ──────────────────
                            📦 Остаток на складе: ${product.stock - quantityToSell} шт.
                        """.trimIndent()
                    }
                }

                // Сбрасываем состояние
                _selectedProduct.value = null
                if (_saleType.value == "sale") {
                    _customerName.value = ""
                }
            } catch (e: Exception) {
                _saleResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    // Добавляем отдельный метод для полного сброса
    fun resetSaleCompletely() {
        _selectedProduct.value = null
        _discount.value = 0.0
        _comment.value = ""
        _saleResult.value = ""
        _searchQuery.value = ""
        _quantity.value = 1
        _paymentSelection.value = setOf("cash")
        _splitCashInput.value = ""
        _splitCardInput.value = ""
        _cardQuery.value = ""
        _selectedCardId.value = null
        _saleType.value = "sale"
        _customerName.value = ""
        _expirationDate.value = null
    }
    // Диалоги
    fun showBrandsDialog() {
        _showBrandsDialog.value = true
    }

    fun hideBrandsDialog() {
        _showBrandsDialog.value = false
    }

    fun showFlavorsDialog() {
        _showFlavorsDialog.value = true
    }

    fun hideFlavorsDialog() {
        _showFlavorsDialog.value = false
    }

    // Сброс продажи
    fun resetSale() {
        _selectedProduct.value = null
        _discount.value = 0.0
        _comment.value = ""
        _saleResult.value = ""  // НЕ очищаем сразу после продажи!
        _searchQuery.value = ""
        _quantity.value = 1
        _paymentSelection.value = setOf("cash")
        _splitCashInput.value = ""
        _splitCardInput.value = ""
        _cardQuery.value = ""
        _selectedCardId.value = null
    }

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    // Тип продажи: "sale" - обычная продажа, "debt" - в долг, "reservation" - резерв
    private val _saleType = MutableStateFlow("sale")
    val saleType: StateFlow<String> = _saleType

    private val _paymentSelection = MutableStateFlow(setOf("cash")) // {"cash"}, {"card"}, {"cash","card"}
    val paymentSelection: StateFlow<Set<String>> = _paymentSelection

    val paymentMethod: StateFlow<String> = paymentSelection
        .map { selection ->
            when {
                selection.contains("cash") && selection.contains("card") -> "split"
                selection.contains("card") -> "card"
                else -> "cash"
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "cash")

    private val _splitCashInput = MutableStateFlow("")
    val splitCashInput: StateFlow<String> = _splitCashInput

    private val _splitCardInput = MutableStateFlow("")
    val splitCardInput: StateFlow<String> = _splitCardInput

    data class SplitPaymentUi(
        val totalDue: Double = 0.0,
        val cash: Double? = null,
        val card: Double? = null,
        val sum: Double? = null,
        val diff: Double? = null,
        val isValid: Boolean = false,
        val error: String? = null
    )

    private data class SplitCalcBase(
        val product: Product?,
        val qty: Int,
        val discount: Double,
        val paymentMethod: String,
        val saleType: String
    )

    private val splitBase: StateFlow<SplitCalcBase> = combine(
        selectedProduct,
        quantity,
        discount,
        paymentMethod,
        saleType
    ) { product, qty, disc, method, currentSaleType ->
        SplitCalcBase(product, qty, disc, method, currentSaleType)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SplitCalcBase(null, 1, 0.0, "cash", "sale")
    )

    val splitPaymentUi: StateFlow<SplitPaymentUi> = combine(
        splitBase,
        splitCashInput,
        splitCardInput
    ) { base, cashInput, cardInput ->
        if (base.saleType != "sale" || base.paymentMethod != "split" || base.product == null) {
            return@combine SplitPaymentUi()
        }

        val totalPrice = base.product.retailPrice * base.qty
        val totalDue = (totalPrice - base.discount).coerceAtLeast(0.0)

        fun parseAmount(raw: String): Double? =
            raw.trim().replace(",", ".").takeIf { it.isNotEmpty() }?.toDoubleOrNull()

        val cash = parseAmount(cashInput)
        val card = parseAmount(cardInput)
        val sum = if (cash != null && card != null) cash + card else null
        val diff = if (sum != null) sum - totalDue else null

        val error = when {
            cash == null || card == null -> "Введите суммы для наличных и карты"
            cash <= 0 || card <= 0 -> "Суммы должны быть больше 0"
            sum == null -> "Введите корректные суммы"
            kotlin.math.abs(sum - totalDue) > 0.01 -> "Суммы должны совпадать с итогом (${String.format("%.2f", totalDue)} BYN)"
            else -> null
        }

        SplitPaymentUi(
            totalDue = totalDue,
            cash = cash,
            card = card,
            sum = sum,
            diff = diff,
            isValid = error == null,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SplitPaymentUi())

    // Для долга/резерва
    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName

    private val _expirationDate = MutableStateFlow<Long?>(null) // Для резерва - дата и время
    val expirationDate: StateFlow<Long?> = _expirationDate

    // НОВЫЕ МЕТОДЫ:
    fun updateQuantity(newQuantity: Int) {
        _quantity.value = newQuantity.coerceAtLeast(1)
    }

    fun updatePaymentMethod(method: String) {
        if (method != "cash" && method != "card") return
        _paymentSelection.value = setOf(method)
        _splitCashInput.value = ""
        _splitCardInput.value = ""
        if (method != "card") {
            _cardQuery.value = ""
            _selectedCardId.value = null
        }
    }

    fun togglePaymentMethod(method: String) {
        if (method != "cash" && method != "card") return
        val current = _paymentSelection.value.toMutableSet()
        if (current.contains(method)) {
            // Нельзя снять последний метод оплаты
            if (current.size == 1) return
            current.remove(method)
        } else {
            current.add(method)
        }
        _paymentSelection.value = current

        // Если вышли из split-режима — очищаем поля, чтобы не было случайных старых значений
        if (paymentMethod.value != "split") {
            _splitCashInput.value = ""
            _splitCardInput.value = ""
        }

        if (!current.contains("card")) {
            _cardQuery.value = ""
            _selectedCardId.value = null
        }
    }

    fun updateSplitCashInput(value: String) {
        _splitCashInput.value = value
    }

    fun updateSplitCardInput(value: String) {
        _splitCardInput.value = value
    }

    fun updateSaleType(type: String) {
        _saleType.value = type
        if (type == "reservation" && _expirationDate.value == null) {
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 7)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            _expirationDate.value = cal.timeInMillis
        }
    }

    fun updateCustomerName(name: String) {
        _customerName.value = name
    }

    fun updateExpirationDate(dateTime: Long?) {
        _expirationDate.value = dateTime
    }
}