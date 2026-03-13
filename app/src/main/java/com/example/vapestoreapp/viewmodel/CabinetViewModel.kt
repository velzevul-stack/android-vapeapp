package com.example.vapestoreapp.viewmodel

import kotlinx.coroutines.withContext
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.data.Sale
import com.example.vapestoreapp.utils.DatabaseExporter
import com.example.vapestoreapp.utils.DatabaseImporter
import com.example.vapestoreapp.utils.StockFormatter
import com.example.vapestoreapp.utils.displayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CabinetViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)
    private val appContext = context.applicationContext

    // Навигация
    private val _currentScreen = MutableStateFlow(CabinetScreen.STOCK)
    val currentScreen: StateFlow<CabinetScreen> = _currentScreen

    // Данные
    private val _stockText = MutableStateFlow("Загрузка...")
    val stockText: StateFlow<String> = _stockText

    private val _totalProfit = MutableStateFlow(0.0)
    val totalProfit: StateFlow<Double> = _totalProfit
    
    private val _totalRevenue = MutableStateFlow(0.0)
    val totalRevenue: StateFlow<Double> = _totalRevenue

    private val _cardRevenueByCard = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val cardRevenueByCard: StateFlow<List<Pair<String, Double>>> = _cardRevenueByCard

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    // Для фильтрации продаж
    private val _selectedPeriod = MutableStateFlow<Period?>(null)
    val selectedPeriod: StateFlow<Period?> = _selectedPeriod

    // Для товаров
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _brandsForCategory = MutableStateFlow<List<String>>(emptyList())
    val brandsForCategory: StateFlow<List<String>> = _brandsForCategory

    // Для редактирования
    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct


    private val _editingBrand = MutableStateFlow<String?>(null)
    val editingBrand: StateFlow<String?> = _editingBrand

    private val _showEditBrandDialog = MutableStateFlow(false)
    val showEditBrandDialog: StateFlow<Boolean> = _showEditBrandDialog

    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog

    // НОВЫЕ: Для группировки продаж по дням
    private val _salesByDay = MutableStateFlow<List<SalesByDay>>(emptyList())
    val salesByDay: StateFlow<List<SalesByDay>> = _salesByDay

    private val _selectedDayForDetails = MutableStateFlow<SalesByDay?>(null)
    val selectedDayForDetails: StateFlow<SalesByDay?> = _selectedDayForDetails

    private val _showDayDetailsDialog = MutableStateFlow(false)
    val showDayDetailsDialog: StateFlow<Boolean> = _showDayDetailsDialog

    private val _dragDropEnabled = MutableStateFlow(false)
    val dragDropEnabled: StateFlow<Boolean> = _dragDropEnabled

    // Поиск в разделе "Товары"
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Массовое изменение цены закупки
    private val _showBulkPriceDialog = MutableStateFlow(false)
    val showBulkPriceDialog: StateFlow<Boolean> = _showBulkPriceDialog

    private val _bulkPriceBrands = MutableStateFlow<List<String>>(emptyList())
    val bulkPriceBrands: StateFlow<List<String>> = _bulkPriceBrands

    private val _bulkPriceResult = MutableStateFlow("")
    val bulkPriceResult: StateFlow<String> = _bulkPriceResult

    // Фильтры для прайс-листа
    private val _stockFilters = MutableStateFlow(StockFilters())
    val stockFilters: StateFlow<StockFilters> = _stockFilters

    private val _showStockFilterDialog = MutableStateFlow(false)
    val showStockFilterDialog: StateFlow<Boolean> = _showStockFilterDialog

    // Экспорт/импорт БД
    private val _backupResult = MutableStateFlow("")
    val backupResult: StateFlow<String> = _backupResult

    init {
        loadAllData()
    }

    private var stockJob: Job? = null
    private var salesJob: Job? = null
    private var allProductsJob: Job? = null
    private var categoriesJob: Job? = null
    private var brandsJob: Job? = null

    private fun loadAllData() {
        loadStockData()
        loadSalesData()
        loadCategories()
        loadAllProducts()
    }

    fun loadAllSales() {
        loadSalesData(null)
    }

    fun enableDragDrop() {
        _dragDropEnabled.value = true
    }

    fun disableDragDrop() {
        _dragDropEnabled.value = false
    }

    suspend fun moveProductInList(fromIndex: Int, toIndex: Int) {
        val category = _selectedCategory.value
        val brand = _selectedBrand.value

        if (category != null) {
            try {
                // Получаем текущие отфильтрованные продукты
                val currentProducts = _filteredProducts.value
                if (fromIndex in currentProducts.indices && toIndex in currentProducts.indices) {
                    // Создаем новую последовательность ID с учетом перемещения
                    val movedProduct = currentProducts[fromIndex]
                    val newOrderList = currentProducts.toMutableList()

                    // Удаляем из старой позиции и вставляем в новую
                    newOrderList.removeAt(fromIndex)
                    newOrderList.add(toIndex, movedProduct)

                    // Обновляем orderIndex для всех товаров в категории
                    val productsInCategory = if (brand != null) {
                        repository.getProductsByBrand(brand).filter { it.category == category }
                    } else {
                        repository.getAllProducts().first().filter { it.category == category }
                    }

                    // Находим соответствующие товары и обновляем их порядок
                    newOrderList.forEachIndexed { index, product ->
                        val productInDb = productsInCategory.find { it.id == product.id }
                        productInDb?.let {
                            repository.updateProductOrder(it.id, index + 1)
                        }
                    }

                    // Обновляем список
                    applyFilter()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Добавим метод для обновления списка после drag & drop
    fun refreshFilteredProducts() {
        applyFilter()
    }

    suspend fun reorderProducts(orderedProducts: List<Product>) {
        val category = _selectedCategory.value
        if (category != null) {
            val orderedIds = orderedProducts.map { it.id }
            repository.reorderProducts(category, orderedIds)
            applyFilter() // Обновляем список после изменения порядка
        }
    }

    // Навигация
    fun navigateTo(screen: CabinetScreen) {
        _currentScreen.value = screen
        when (screen) {
            CabinetScreen.STOCK -> loadStockData()
            CabinetScreen.SALES -> loadAllSales() // Используем новый метод
            CabinetScreen.PRODUCTS -> {
                loadCategories()
                applyFilter()
            }
            CabinetScreen.SALES_MANAGEMENT -> {
                // Редактирование продаж - данные загружаются через SalesManagementViewModel
            }
        }
    }

    // Загрузка данных склада (с учётом резервов)
    // Flow автоматически обновляется при изменении данных в БД (Room)
    private fun loadStockData() {
        stockJob?.cancel()
        stockJob = viewModelScope.launch {
            repository.getAllProducts()
                .combine(repository.getReservedQuantityByProductFlow()) { products, reserved ->
                    products to reserved
                }
                .collect { (products, reserved) ->
                    _stockText.value = formatStockByCategories(products, reserved)
                }
        }
    }
    // Helper-функция для вычисления позиции дропа
    private fun calculateDropPosition(
        fromIndex: Int,
        dragOffset: Float,
        itemHeight: Float = 70f // примерная высота элемента
    ): Int {
        val itemsToMove = (dragOffset / itemHeight).toInt()
        return (fromIndex + itemsToMove).coerceAtLeast(0)
    }

    // Форматирование склада по категориям (с учётом резервов и фильтров)
    private fun formatStockByCategories(products: List<Product>, reservedByProduct: Map<Int, Int> = emptyMap()): String {
        var filteredProducts = products
        
        // Применяем фильтры
        val filters = _stockFilters.value
        if (!filters.isEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                // Фильтр по категориям
                if (filters.selectedCategories.isNotEmpty() && product.category !in filters.selectedCategories) {
                    return@filter false
                }
                
                // Фильтр по брендам
                if (filters.selectedBrands.isNotEmpty() && product.brand !in filters.selectedBrands) {
                    return@filter false
                }
                
                // Фильтр по mg (извлекаем из бренда или спецификации)
                if (filters.selectedMgValues.isNotEmpty()) {
                    val mgInBrand = extractMgFromText(product.brand)
                    val mgInSpec = extractMgFromText(product.specification)
                    val mgInStrength = extractMgFromText(product.strength)
                    val allMgValues = setOf(mgInBrand, mgInSpec, mgInStrength).filterNotNull()
                    
                    if (allMgValues.none { it in filters.selectedMgValues }) {
                        return@filter false
                    }
                }
                
                // Фильтр по цене
                if (filters.minPrice != null && product.retailPrice < filters.minPrice) {
                    return@filter false
                }
                if (filters.maxPrice != null && product.retailPrice > filters.maxPrice) {
                    return@filter false
                }
                
                // Фильтр по наличию
                if (filters.hasStock && product.stock <= 0) {
                    return@filter false
                }
                
                true
            }
        } else if (filters.hasStock) {
            filteredProducts = filteredProducts.filter { it.stock > 0 }
        }
        
        return StockFormatter.formatStockForCabinet(filteredProducts, reservedByProduct)
    }
    
    // Извлечение mg из текста (например "50mg" -> "50")
    private fun extractMgFromText(text: String): String? {
        val regex = Regex("(\\d+)mg", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)
    }
    
    fun openStockFilterDialog() {
        _showStockFilterDialog.value = true
    }
    
    fun closeStockFilterDialog() {
        _showStockFilterDialog.value = false
    }
    
    fun updateStockFilters(filters: StockFilters) {
        _stockFilters.value = filters
        // Обновляем склад с новыми фильтрами
        viewModelScope.launch {
            val products = repository.getAllProducts().first()
            val reserved = repository.getReservedQuantityByProduct()
            _stockText.value = formatStockByCategories(products, reserved)
        }
    }
    
    fun resetStockFilters() {
        _stockFilters.value = StockFilters()
        viewModelScope.launch {
            val products = repository.getAllProducts().first()
            val reserved = repository.getReservedQuantityByProduct()
            _stockText.value = formatStockByCategories(products, reserved)
        }
    }
    
    // Получение доступных значений для фильтров
    suspend fun getAvailableCategories(): List<String> {
        val products = repository.getAllProducts().first()
        return products.map { it.category }.distinct().sorted()
    }
    
    suspend fun getAvailableBrands(): List<String> {
        val products = repository.getAllProducts().first()
        return products.map { it.brand }.distinct().sorted()
    }
    
    suspend fun getAvailableMgValues(): List<String> {
        val products = repository.getAllProducts().first()
        val mgSet = mutableSetOf<String>()
        products.forEach { product ->
            extractMgFromText(product.brand)?.let { mgSet.add(it) }
            extractMgFromText(product.specification)?.let { mgSet.add(it) }
            extractMgFromText(product.strength)?.let { mgSet.add(it) }
        }
        return mgSet.sortedBy { it.toIntOrNull() ?: 0 }
    }
    
    suspend fun getPriceRange(): Pair<Double, Double> {
        val products = repository.getAllProducts().first()
        val prices = products.map { it.retailPrice }.filter { it > 0 }
        return if (prices.isNotEmpty()) {
            Pair(prices.minOrNull() ?: 0.0, prices.maxOrNull() ?: 0.0)
        } else {
            Pair(0.0, 0.0)
        }
    }

    // Загрузка продаж с группировкой по дням
    fun loadSalesData(period: Period? = null) {
        _selectedPeriod.value = period

        salesJob?.cancel()
        salesJob = viewModelScope.launch {
            val salesFlow = if (period != null) {
                repository.getSalesInPeriod(period.start, period.end)
            } else {
                repository.getAllSales()
            }

            salesFlow
                .combine(repository.getAllProducts()) { sales, products -> sales to products }
                .combine(repository.getAllPaymentCards()) { (sales, products), cards -> Triple(sales, products, cards) }
                .collect { (sales, products, cards) ->
                val productMap = products.associateBy { it.id }
                val cardMap = cards.associateBy { it.id }

                // Группируем продажи по дням
                val salesByDayMap = sales
                    .filter { !it.isCancelled } // Только активные продажи
                    .groupBy { sale ->
                        // Приводим к началу дня для группировки
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = sale.date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        calendar.timeInMillis
                    }
                    .toSortedMap(compareByDescending { it }) // Сортируем по дате (новые сверху)

                val displaySalesByDay = mutableListOf<SalesByDay>()

                salesByDayMap.forEach { (dayTimestamp, daySales) ->
                    // Нумеруем продажи внутри дня, начиная с 1
                    val numberedDaySales = daySales
                        .sortedBy { it.date } // Сортируем по времени внутри дня
                        .mapIndexed { index, sale ->
                            val product = productMap[sale.productId]
                            val cardLabel = sale.cardId?.let { cardMap[it]?.label }
                            SaleDisplay(
                                sale = sale,
                                productName = product?.displayName() ?: "Товар #${sale.productId}",
                                formattedDate = formatDate(sale.date),
                                saleNumberInDay = index + 1, // Нумерация с 1
                                totalSalesInDay = daySales.size, // Всего продаж в этот день
                                cardLabel = cardLabel
                            )
                        }

                    fun cashRevenuePart(sale: Sale): Double = when (sale.paymentMethod) {
                        "cash" -> sale.revenue
                        "split" -> sale.cashAmount ?: 0.0
                        else -> 0.0
                    }

                    fun cardRevenuePart(sale: Sale): Double = when (sale.paymentMethod) {
                        "card" -> sale.revenue
                        "split" -> sale.cardAmount ?: 0.0
                        else -> 0.0
                    }

                    fun cashProfitPart(sale: Sale): Double = when (sale.paymentMethod) {
                        "cash" -> sale.profit
                        "split" -> {
                            val cash = sale.cashAmount ?: 0.0
                            val rev = sale.revenue
                            if (rev > 0) sale.profit * (cash / rev) else 0.0
                        }
                        else -> 0.0
                    }

                    fun cardProfitPart(sale: Sale): Double = when (sale.paymentMethod) {
                        "card" -> sale.profit
                        "split" -> {
                            val card = sale.cardAmount ?: 0.0
                            val rev = sale.revenue
                            if (rev > 0) sale.profit * (card / rev) else 0.0
                        }
                        else -> 0.0
                    }
                    
                    // Вычисляем выручку по картам для этого дня
                    val dayCardRevenueByCard = mutableMapOf<String, Double>()
                    daySales.forEach { sale ->
                        val cardPart = when (sale.paymentMethod) {
                            "card" -> sale.revenue
                            "split" -> sale.cardAmount ?: 0.0
                            else -> 0.0
                        }
                        if (cardPart > 0.0) {
                            val label = sale.cardId?.let { cardMap[it]?.label } ?: "Неизвестная карта"
                            dayCardRevenueByCard[label] = (dayCardRevenueByCard[label] ?: 0.0) + cardPart
                        }
                    }
                    
                    displaySalesByDay.add(
                        SalesByDay(
                            dayTimestamp = dayTimestamp,
                            formattedDate = formatDate(dayTimestamp, true), // Только дата
                            daySales = numberedDaySales,
                            dayProfit = daySales.sumOf { it.profit },
                            dayRevenue = daySales.sumOf { it.revenue },
                            cashRevenue = daySales.sumOf { cashRevenuePart(it) },
                            cardRevenue = daySales.sumOf { cardRevenuePart(it) },
                            cashProfit = daySales.sumOf { cashProfitPart(it) },
                            cardProfit = daySales.sumOf { cardProfitPart(it) },
                            totalSales = daySales.size,
                            cardRevenueByCard = dayCardRevenueByCard
                        )
                    )
                }

                _salesByDay.value = displaySalesByDay
                _totalProfit.value = sales.sumOf { it.profit }
                _totalRevenue.value = sales.sumOf { it.revenue }

                val activeSales = sales.filter { !it.isCancelled }
                val byCard = mutableMapOf<String, Double>()
                activeSales.forEach { sale ->
                    val cardPart = when (sale.paymentMethod) {
                        "card" -> sale.revenue
                        "split" -> sale.cardAmount ?: 0.0
                        else -> 0.0
                    }
                    if (cardPart <= 0.0) return@forEach
                    val label = sale.cardId?.let { cardMap[it]?.label } ?: "Неизвестная карта"
                    byCard[label] = (byCard[label] ?: 0.0) + cardPart
                }
                _cardRevenueByCard.value = byCard.entries
                    .sortedByDescending { it.value }
                    .map { it.key to it.value }
            }
        }
    }

    // Форматирование даты
    private fun formatDate(timestamp: Long, dateOnly: Boolean = false): String {
        val sdf = if (dateOnly) {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        } else {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        }
        return sdf.format(Date(timestamp))
    }

    // Загрузка всех товаров
    private fun loadAllProducts() {
        if (allProductsJob != null) return
        allProductsJob = viewModelScope.launch {
            repository.getAllProducts().collect { products ->
                _allProducts.value = products
                // Если пользователь в разделе "Товары" — пересчитываем фильтр автоматически
                if (_currentScreen.value == CabinetScreen.PRODUCTS) {
                    applyFilter()
                }
            }
        }
    }


    fun moveBrandUpOptimized(category: String, brand: String) {
        viewModelScope.launch(Dispatchers.IO) { // Используем IO dispatcher для работы с БД
            try {
                // Получаем товары только этой категории из базы
                val allProducts = repository.getAllProducts().first()
                val categoryProducts = allProducts.filter { it.category == category }

                if (categoryProducts.isEmpty()) return@launch

                // Группируем по брендам
                val productsByBrand = categoryProducts.groupBy { it.brand }

                // Получаем список брендов в текущем порядке
                val brandList = mutableListOf<String>()
                val productList = mutableListOf<Product>()

                // Собираем все товары в правильном порядке
                categoryProducts.sortedBy { it.orderIndex }.forEach { product ->
                    if (!brandList.contains(product.brand)) {
                        brandList.add(product.brand)
                    }
                    productList.add(product)
                }

                // Находим текущий индекс бренда
                val currentIndex = brandList.indexOf(brand)
                if (currentIndex <= 0) return@launch

                // Меняем бренды местами
                brandList[currentIndex] = brandList[currentIndex - 1]
                brandList[currentIndex - 1] = brand

                // Пересчитываем orderIndex для всех товаров в категории
                val productsToUpdate = mutableListOf<Product>()
                var orderCounter = 1

                brandList.forEach { brandName ->
                    val brandProducts = productsByBrand[brandName] ?: emptyList()
                    brandProducts.forEach { product ->
                        productsToUpdate.add(product.copy(orderIndex = orderCounter++))
                    }
                }

                // Пакетное обновление
                if (productsToUpdate.isNotEmpty()) {
                    repository.updateProducts(productsToUpdate)
                }

                // Обновляем UI в главном потоке
                withContext(Dispatchers.Main) {
                    applyFilter()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveBrandDownOptimized(category: String, brand: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Получаем товары только этой категории из базы
                val allProducts = repository.getAllProducts().first()
                val categoryProducts = allProducts.filter { it.category == category }

                if (categoryProducts.isEmpty()) return@launch

                // Группируем по брендам
                val productsByBrand = categoryProducts.groupBy { it.brand }

                // Получаем список брендов в текущем порядке
                val brandList = mutableListOf<String>()
                val productList = mutableListOf<Product>()

                // Собираем все товары в правильном порядке
                categoryProducts.sortedBy { it.orderIndex }.forEach { product ->
                    if (!brandList.contains(product.brand)) {
                        brandList.add(product.brand)
                    }
                    productList.add(product)
                }

                // Находим текущий индекс бренда
                val currentIndex = brandList.indexOf(brand)
                if (currentIndex >= brandList.size - 1) return@launch

                // Меняем бренды местами
                brandList[currentIndex] = brandList[currentIndex + 1]
                brandList[currentIndex + 1] = brand

                // Пересчитываем orderIndex для всех товаров в категории
                val productsToUpdate = mutableListOf<Product>()
                var orderCounter = 1

                brandList.forEach { brandName ->
                    val brandProducts = productsByBrand[brandName] ?: emptyList()
                    brandProducts.forEach { product ->
                        productsToUpdate.add(product.copy(orderIndex = orderCounter++))
                    }
                }

                // Пакетное обновление
                if (productsToUpdate.isNotEmpty()) {
                    repository.updateProducts(productsToUpdate)
                }

                // Обновляем UI в главном потоке
                withContext(Dispatchers.Main) {
                    applyFilter()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Загрузка категорий
    private fun loadCategories() {
        if (categoriesJob != null) return
        categoriesJob = viewModelScope.launch {
            repository.getAllProducts().collect { products ->
                val cats = products.map { it.category }
                    .distinct()
                    .sorted()
                    .map { getCategoryDisplayName(it) }
                _categories.value = cats
            }
        }
    }

    // Выбор категории
    fun selectCategory(category: String?) {
        val actualCategory = when (category) {
            "🍬 Жидкость" -> "liquid"
            "🚬 Одноразка" -> "disposable"
            "⚙️ Расходник" -> "consumable"
            "🔋 Вейп" -> "vape"
            " Снюс" -> "snus"
            else -> null
        }

        _selectedCategory.value = actualCategory
        _selectedBrand.value = null

        if (actualCategory != null) {
            loadBrandsForCategory(actualCategory)
        } else {
            _brandsForCategory.value = emptyList()
            brandsJob?.cancel()
            brandsJob = null
        }
        applyFilter()
    }

    // Загрузка брендов для категории
    private fun loadBrandsForCategory(category: String) {
        brandsJob?.cancel()
        brandsJob = viewModelScope.launch {
            repository.getBrandsByCategory(category).collect { brands ->
                _brandsForCategory.value = brands
            }
        }
    }

    // Методы для управления диалогами детализации
    fun openDayDetails(day: SalesByDay) {
        _selectedDayForDetails.value = day
        _showDayDetailsDialog.value = true
    }

    fun closeDayDetails() {
        _selectedDayForDetails.value = null
        _showDayDetailsDialog.value = false
    }

    // Выбор бренда
    fun selectBrand(brand: String?) {
        _selectedBrand.value = brand
        applyFilter()
    }

    // Применение фильтра (с учётом поиска)
    private fun applyFilter() {
        viewModelScope.launch {
            val allProducts = _allProducts.value
            val query = _searchQuery.value.trim().lowercase()

            val filtered = allProducts.filter { product ->
                (selectedCategory.value == null || product.category == selectedCategory.value) &&
                        (selectedBrand.value == null || product.brand == selectedBrand.value) &&
                        (query.isEmpty() || product.brand.lowercase().contains(query) ||
                                product.flavor.lowercase().contains(query) ||
                                product.specification.lowercase().contains(query))
            }.sortedBy { it.orderIndex }

            _filteredProducts.value = filtered
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    // Отображение категории
    private fun getCategoryDisplayName(category: String): String {
        return when (category) {
            "liquid" -> "🍬 Жидкость"
            "disposable" -> "🚬 Одноразка"
            "consumable" -> "⚙️ Расходник"
            "vape" -> "🔋 Вейп"
            "snus" -> " Снюс"
            else -> category
        }
    }

    // Обновление товаров
    fun updateProductStock(productId: Int, newStock: Int) {
        viewModelScope.launch {
            val product = _allProducts.value.find { it.id == productId }
            product?.let {
                repository.updateProduct(it.copy(stock = newStock))
                loadAllData() // Обновляем все данные
            }
        }
    }

    // Диалоги редактирования
    fun openEditDialog(product: Product) {
        _editingProduct.value = product
        _showAddEditDialog.value = true
    }

    fun openAddDialog() {
        _editingProduct.value = null
        _showAddEditDialog.value = true
    }

    // Для открытия диалога редактирования бренда
    fun openEditBrandDialog(brand: String) {
        _editingBrand.value = brand
        _showEditBrandDialog.value = true
    }

    // Для закрытия диалога
    fun closeEditBrandDialog() {
        _showEditBrandDialog.value = false
        _editingBrand.value = null
    }

    // Для обновления всех товаров бренда
    fun updateBrand(oldBrand: String, newBrand: String) {
        viewModelScope.launch {
            try {
                // Получаем все товары со старым брендом
                val allProducts = repository.getAllProducts().first()
                val productsWithOldBrand = allProducts.filter { it.brand == oldBrand }

                // Обновляем каждый товар с новым брендом
                productsWithOldBrand.forEach { product ->
                    repository.updateProduct(product.copy(brand = newBrand))
                }

                // Обновляем данные
                loadAllData()
                closeEditBrandDialog()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun closeAddEditDialog() {
        _showAddEditDialog.value = false
        _editingProduct.value = null
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            loadAllData()
            closeAddEditDialog()
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            loadAllData()
        }
    }

    fun refreshStock() {
        loadStockData()
    }

    // Массовое изменение цены закупки
    fun openBulkPriceDialog() {
        viewModelScope.launch {
            val products = repository.getAllProducts().first()
            val brands = products.map { it.brand }.distinct().sorted()
            _bulkPriceBrands.value = brands
            _bulkPriceResult.value = ""
            _showBulkPriceDialog.value = true
        }
    }

    fun closeBulkPriceDialog() {
        _showBulkPriceDialog.value = false
        _bulkPriceResult.value = ""
    }

    fun applyBulkPriceChange(brand: String, newPrice: Double) {
        viewModelScope.launch {
            try {
                repository.updatePurchasePriceForBrand(brand, newPrice)
                _bulkPriceResult.value = "✅ Цена закупки для $brand обновлена"
                loadAllData()
            } catch (e: Exception) {
                _bulkPriceResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    // Экспорт/импорт БД
    fun exportDatabase() {
        viewModelScope.launch {
            _backupResult.value = "Экспорт..."
            try {
                val result = DatabaseExporter.exportDatabase(appContext, repository)
                _backupResult.value = if (result != null) {
                    when {
                        result.downloadsUri != null -> "✅ Экспортировано в Загрузки/VapeStoreBackups:\n${result.fileName}"
                        result.legacyFile != null -> "✅ Данные экспортированы:\n${result.legacyFile.absolutePath}"
                        else -> "❌ Ошибка экспорта"
                    }
                } else {
                    "❌ Ошибка экспорта"
                }
            } catch (e: Exception) {
                _backupResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun importDatabase(backupFile: java.io.File) {
        viewModelScope.launch {
            _backupResult.value = "Импорт..."
            try {
                val success = DatabaseImporter.importDatabase(appContext, backupFile, repository)
                _backupResult.value = if (success) {
                    "✅ Данные импортированы"
                } else {
                    "❌ Ошибка импорта"
                }
                if (success) loadAllData()
            } catch (e: Exception) {
                _backupResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun importFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            _backupResult.value = "Импорт..."
            try {
                val success = DatabaseImporter.importDatabase(appContext, uri, repository)
                _backupResult.value = if (success) {
                    "✅ Данные импортированы"
                } else {
                    "❌ Ошибка импорта"
                }
                if (success) loadAllData()
            } catch (e: Exception) {
                _backupResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }

    fun clearBackupResult() {
        _backupResult.value = ""
    }

    // Очистка всего склада
    fun clearAllStock() {
        viewModelScope.launch {
            try {
                repository.clearAllStock()
                _backupResult.value = "✅ Склад полностью очищен"
                loadAllData() // Обновляем данные после очистки
            } catch (e: Exception) {
                _backupResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }
}

// Экраны кабинета
enum class CabinetScreen {
    STOCK, SALES, PRODUCTS, SALES_MANAGEMENT
}

// Фильтры для прайс-листа
data class StockFilters(
    val selectedCategories: Set<String> = emptySet(),
    val selectedBrands: Set<String> = emptySet(),
    val selectedMgValues: Set<String> = emptySet(), // "20", "30", "50" и т.д.
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val hasStock: Boolean = true // Только товары с остатком
) {
    fun isEmpty(): Boolean {
        return selectedCategories.isEmpty() &&
                selectedBrands.isEmpty() &&
                selectedMgValues.isEmpty() &&
                minPrice == null &&
                maxPrice == null &&
                hasStock
    }
}

// Период для фильтрации
data class Period(
    val name: String,
    val start: Long,
    val end: Long
) {
    companion object {
        fun getSeasons(): List<Period> {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)

            return listOf(
                Period(
                    name = "Осень",
                    start = getSeasonStart(currentYear, 9), // Сентябрь
                    end = getSeasonEnd(currentYear, 11)     // Ноябрь
                ),
                Period(
                    name = "Зима",
                    start = getSeasonStart(currentYear, 12), // Декабрь
                    end = getSeasonEnd(currentYear + 1, 2)   // Февраль следующего года
                ),
                Period(
                    name = "Весна",
                    start = getSeasonStart(currentYear, 3), // Март
                    end = getSeasonEnd(currentYear, 5)      // Май
                ),
                Period(
                    name = "Лето",
                    start = getSeasonStart(currentYear, 6), // Июнь
                    end = getSeasonEnd(currentYear, 8)      // Август
                )
            )
        }

        private fun getSeasonStart(year: Int, month: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        private fun getSeasonEnd(year: Int, month: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }

        // Метод для получения произвольного периода
        fun getCustomPeriod(startDate: Date, endDate: Date): Period {
            val calendar = Calendar.getInstance()

            // Начало дня для startDate
            calendar.time = startDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            // Конец дня для endDate
            calendar.time = endDate
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val end = calendar.timeInMillis

            return Period(
                name = "Произвольный период",
                start = start,
                end = end
            )
        }
    }
}

// Отображение продажи с красивым форматом
data class SaleDisplay(
    val sale: Sale,
    val productName: String,
    val formattedDate: String,
    val saleNumberInDay: Int = 0, // Номер продажи в дне
    val totalSalesInDay: Int = 0, // Всего продаж в этот день
    val cardLabel: String? = null // Название карты, если оплата по карте
)

// Добавляем новые data class для структурированных данных по дням
data class SalesByDay(
    val dayTimestamp: Long,
    val formattedDate: String,
    val daySales: List<SaleDisplay>,
    val dayProfit: Double,
    val dayRevenue: Double,
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0,
    val cashProfit: Double = 0.0,
    val cardProfit: Double = 0.0,
    val totalSales: Int,
    val cardRevenueByCard: Map<String, Double> = emptyMap() // Выручка по каждой карте за день
)