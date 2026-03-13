package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.utils.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AcceptViewModel(private val context: Context) : ViewModel() {
    private val repository = Repository(context)

    // Состояния UI
    private val _barcodeInput = MutableStateFlow("")
    val barcodeInput: StateFlow<String> = _barcodeInput

    private val _searchResult = MutableStateFlow("")
    val searchResult: StateFlow<String> = _searchResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _showBrandsDialog = MutableStateFlow(false)
    val showBrandsDialog: StateFlow<Boolean> = _showBrandsDialog

    private val _showFlavorsDialog = MutableStateFlow(false)
    val showFlavorsDialog: StateFlow<Boolean> = _showFlavorsDialog

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand

    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands

    private val _flavors = MutableStateFlow<List<Product>>(emptyList())
    val flavors: StateFlow<List<Product>> = _flavors

    private val _filteredBrands = MutableStateFlow<List<String>>(emptyList())
    val filteredBrands: StateFlow<List<String>> = _filteredBrands

    private val _showBrandsDialogForNewProduct = MutableStateFlow(false)
    val showBrandsDialogForNewProduct: StateFlow<Boolean> = _showBrandsDialogForNewProduct

    private val _isInAddDialogContext = MutableStateFlow(false)
    val isInAddDialogContext: StateFlow<Boolean> = _isInAddDialogContext

    private val _showGiftProductDialog = MutableStateFlow(false)
    val showGiftProductDialog: StateFlow<Boolean> = _showGiftProductDialog

    private val _giftProductData = MutableStateFlow(GiftProductData())
    val giftProductData: StateFlow<GiftProductData> = _giftProductData

    // Данные для нового товара
    private val _newProductData = MutableStateFlow(NewProductData())
    val newProductData: StateFlow<NewProductData> = _newProductData

    private val _allBrands = MutableStateFlow<List<String>>(emptyList())
    val allBrands: StateFlow<List<String>> = _allBrands

    private val _allProductsByBrand = MutableStateFlow<List<Product>>(emptyList())
    val allProductsByBrand: StateFlow<List<Product>> = _allProductsByBrand

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    // Для загрузки ВСЕХ категорий
    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    val allCategories: StateFlow<List<String>> = _allCategories
    
    private val _brandDuplicateWarning = MutableStateFlow<String>("")
    val brandDuplicateWarning: StateFlow<String> = _brandDuplicateWarning

    // Категории товаров
    val categories = listOf(
        "liquid" to "🍬 Жидкость",
        "disposable" to "🚬 Одноразка",
        "consumable" to "⚙️ Расходник",
        "vape" to "🔋 Вейп",
        "snus" to "Снюсик <3"
    )

    // Крепости для жидкостей
    val strengths = listOf(
        "" to "Без крепости",
        "20mg" to "20mg",
        "35mg" to "35mg",
        "50mg" to "50mg",
        "60mg" to "60mg"
    )

    init {
        loadAllBrands()
        loadBrands()
        loadAllCategories()
        loadAllProducts()
    }
    
    private fun loadAllProducts() {
        viewModelScope.launch {
            repository.getAllProducts().collect { products ->
                _allProducts.value = products
            }
        }
    }

    private val acceptMutex = Mutex()
    private var lastAcceptedBarcode: String? = null
    private var lastAcceptedAtMs: Long = 0L
    private val acceptDebounceMs = 350L




    fun loadBrandsByCategory(category: String) {
        viewModelScope.launch {
            repository.getBrandsByCategory(category).collect { brands ->
                _filteredBrands.value = brands  // Загружаем бренды для выбранной категории
            }
        }
    }

    fun showBrandsDialogForNewProduct() {
        // Используем filteredBrands (бренды ТОЛЬКО текущей категории)
        // Они уже загружаются через loadBrandsByCategory()
        // showBrandsDialogForNewProduct не должен перезаписывать filteredBrands!
        _showBrandsDialogForNewProduct.value = true
    }


    fun hideBrandsDialogForNewProduct() {
        _showBrandsDialogForNewProduct.value = false
    }

    fun selectBrandForNewProduct(brand: String) {
        updateNewProductData(_newProductData.value.copy(brand = brand))
        hideBrandsDialogForNewProduct()
    }

    fun setAddDialogContext(isInDialog: Boolean) {
        _isInAddDialogContext.value = isInDialog
    }

    private fun loadBrands() {
        viewModelScope.launch {
            // ИСПРАВЛЕНО: используем правильный метод
            repository.getBrandsWithStock().collect { brandsList ->
                println("✅ AcceptViewModel: Загружено брендов с остатком: ${brandsList.size}")
                brandsList.forEachIndexed { i, brand -> println("  $i. $brand") }
                _brands.value = brandsList
            }
        }
    }

    fun handleBarcodeInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        val normalized = if (digitsOnly.length > 13) digitsOnly.take(13) else digitsOnly
        _barcodeInput.value = normalized

        // Автоприёмка только при полном штрих-коде (13 цифр)
        if (normalized.length == 13) {
            submitBarcode(normalized)
        }
    }

    fun submitBarcode(rawBarcode: String) {
        val barcode = rawBarcode.filter { it.isDigit() }.take(13)
        if (barcode.length != 13) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (barcode == lastAcceptedBarcode && (now - lastAcceptedAtMs) < acceptDebounceMs) return@launch
            lastAcceptedBarcode = barcode
            lastAcceptedAtMs = now

            acceptMutex.withLock {
                acceptByBarcodeInternal(barcode)
            }
        }
    }


    fun loadAllProductsByBrand(brand: String) {
        viewModelScope.launch {
            val products = repository.getProductsByBrand(brand) // Этот метод должен быть в Repository
            _allProductsByBrand.value = products
        }
    }


    fun loadAllCategories() {
        viewModelScope.launch {
            // Логика загрузки всех категорий
            // Можно просто использовать фиксированный список или загрузить из базы
            _allCategories.value = listOf("liquid", "disposable", "consumable", "vape", "snus")
        }
    }

    fun acceptByBarcode(barcode: String) {
        submitBarcode(barcode)
    }

    private suspend fun acceptByBarcodeInternal(barcode: String) {
        _isLoading.value = true

        // Очищаем поле ввода сразу, чтобы следующий скан не склеился с предыдущим
        _barcodeInput.value = ""

        val product = repository.getProductByBarcode(barcode)

        if (product != null) {
            repository.updateProduct(product.copy(stock = product.stock + 1))
            _searchResult.value = "✅ Добавлено: ${formatProductName(product)} (теперь: ${product.stock + 1} шт.)"
        } else {
            _searchResult.value = "❗ Товар с кодом $barcode не найден"
            _newProductData.value = NewProductData(barcode = barcode)
        }
        _isLoading.value = false
    }

    private fun formatProductName(product: Product): String {
        return product.displayName()
    }

    fun selectExistingBrand(brandName: String) {
        _newProductData.value = _newProductData.value.copy(brand = brandName)
    }

    fun selectBrand(brand: String) {
        _selectedBrand.value = brand
        _showBrandsDialog.value = false
        loadFlavors(brand)
        _showFlavorsDialog.value = true
    }

    private fun loadFlavors(brand: String) {
        viewModelScope.launch {
            _flavors.value = repository.getFlavorsByBrand(brand)
        }
    }

    fun selectFlavor(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(stock = product.stock + 1))
            _searchResult.value = "✅ Добавлено: ${formatProductName(product)} (теперь: ${product.stock + 1} шт.)"
        }
        _showFlavorsDialog.value = false
    }

    fun addNewProduct() {
        val data = _newProductData.value

        if (data.brand.isBlank()) {
            _searchResult.value = "❌ Ошибка: заполните бренд"
            return
        }

        // Проверка на дубликаты брендов (если предупреждение было проигнорировано)
        viewModelScope.launch {
            val existingBrands = repository.getAllBrands().first()
            val normalizedNewBrand = normalizeBrand(data.brand)
            
            val similarBrands = existingBrands.filter { existingBrand ->
                val normalizedExisting = normalizeBrand(existingBrand)
                isBrandSimilar(normalizedNewBrand, normalizedExisting)
            }
            
            if (similarBrands.isNotEmpty() && !similarBrands.contains(data.brand)) {
                _searchResult.value = "⚠️ Похожий бренд уже существует:\n${similarBrands.first()}\n\nИспользуйте существующий бренд или измените название."
                return@launch
            }
            
            addNewProductInternal(data)
        }
    }
    
    // Нормализация бренда для сравнения
    private fun normalizeBrand(brand: String): String {
        return brand.trim()
            .lowercase()
            .replace(Regex("[^а-яa-z0-9]"), "") // Убираем спецсимволы
            .replace(Regex("\\s+"), "") // Убираем пробелы
    }
    
    // Проверка похожести брендов
    private fun isBrandSimilar(brand1: String, brand2: String): Boolean {
        if (brand1 == brand2) return true
        
        // Проверка на очень похожие названия (разница в 1-2 символа)
        val diff = levenshteinDistance(brand1, brand2)
        val maxLength = maxOf(brand1.length, brand2.length)
        
        // Если разница менее 15% от длины или абсолютно менее 3 символов
        return diff <= 2 || (maxLength > 5 && diff.toDouble() / maxLength < 0.15)
    }
    
    // Расстояние Левенштейна для сравнения строк
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(
                        dp[i - 1][j] + 1,      // удаление
                        dp[i][j - 1] + 1,      // вставка
                        dp[i - 1][j - 1] + 1   // замена
                    )
                }
            }
        }
        
        return dp[m][n]
    }
    
    private suspend fun addNewProductInternal(data: NewProductData) {
        when (data.category) {
            "liquid", "disposable", "snus" -> {
                if (data.flavor.isBlank()) {
                    _searchResult.value = "❌ Ошибка: для ${getCategoryName(data.category)} укажите вкус"
                    return
                }
            }
            "vape" -> {
                if (data.specification.isBlank()) {
                    _searchResult.value = "❌ Ошибка: для вейпа укажите цвет"
                    return
                }
            }
            "consumable" -> {
                // Для расходников вкус не обязателен
            }
        }

        viewModelScope.launch {
            val finalBrand = when (data.category) {
                "liquid" -> {
                    val strengthText = if (data.strength.isNotEmpty()) " ${data.strength}mg" else ""
                    "${data.brand}$strengthText"
                }
                else -> data.brand
            }

            // Защита от null/пустых значений для snus и disposable
            val finalFlavor = when (data.category) {
                "liquid", "disposable", "snus" -> data.flavor.ifBlank { data.brand }
                "vape" -> "" // Для вейпов вкус не используем (цвет хранится в specification)
                "consumable" -> data.flavor.ifBlank { "" }
                else -> data.flavor.ifBlank { "" }
            }

            // Для вейпа указываем цвет в спецификации (защита от null)
            val specificationText = when (data.category) {
                "vape" -> data.specification.ifBlank { "Стандарт" }
                else -> data.specification.ifBlank { "" }
            }

            // ВАЖНО: Проверяем, что цены корректные
            val purchasePrice = if (data.isGift && data.purchasePrice.isBlank()) {
                0.0 // Подарочный товар - цена закупки 0
            } else {
                data.purchasePrice.toDoubleOrNull() ?: 0.0
            }
            val retailPrice = data.retailPrice.toDoubleOrNull() ?: 0.0

            if (data.category == "snus" && data.flavor.isBlank()) {
                _searchResult.value = "❌ Ошибка: для снюса укажите вкус"
                return@launch
            }

            try {
                var addedCount = 0
                val quantity = if (data.category == "liquid" && data.quantity > 1) data.quantity else 1
                
                // Добавляем несколько позиций если нужно (для жидкостей с разными ценами)
                repeat(quantity) {
            val newProduct = Product(
                brand = finalBrand,
                flavor = finalFlavor,
                        barcode = if (data.barcode.isNotBlank() && quantity == 1) data.barcode.ifBlank { null } else null,
                purchasePrice = purchasePrice,
                retailPrice = retailPrice,
                stock = 1,
                category = data.category,
                strength = data.strength,
                specification = specificationText,
                        orderIndex = 0
            )

                repository.insertProduct(newProduct)
                    addedCount++
                }

                val giftText = if (data.isGift) " (подарочный)" else ""
                _searchResult.value = if (quantity > 1) {
                    "🎉 Добавлено $quantity позиций:\n$finalBrand$giftText"
                } else {
                    "🎉 Добавлен новый товар:\n$finalBrand$giftText"
                }

                if (data.category == "snus") {
                    _searchResult.value += "\nВкус: ${data.flavor}"
                }
                
                if (purchasePrice == 0.0 && data.isGift) {
                    _searchResult.value += "\n💰 Закупочная цена: 0 BYN (подарок)"
                }

                _newProductData.value = NewProductData()
                _showAddDialog.value = false
                loadBrands()

            } catch (e: Exception) {
                _searchResult.value = "❌ Ошибка при добавлении: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // Вспомогательная функция для названия категории
    private fun getCategoryName(category: String): String {
        return when (category) {
            "liquid" -> "жидкости"
            "disposable" -> "одноразки"
            "snus" -> "снюса"
            else -> category
        }
    }

    private fun loadAllBrands() {
        viewModelScope.launch {
            repository.getAllBrands().collect { brands ->
                _allBrands.value = brands
            }
        }
    }

    fun showBrandsDialog() {
        _showBrandsDialog.value = true
    }

    fun hideBrandsDialog() { _showBrandsDialog.value = false }
    fun showFlavorsDialog() { _showFlavorsDialog.value = true }
    fun hideFlavorsDialog() { _showFlavorsDialog.value = false }
    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun clearResult() {
        _searchResult.value = ""
        _barcodeInput.value = ""
    }

    fun updateNewProductData(newData: NewProductData) {
        _newProductData.value = newData
        
        // Проверка на дубликаты брендов в реальном времени
        if (newData.brand.isNotBlank() && newData.brand.length >= 3) {
            viewModelScope.launch {
                checkBrandDuplicates(newData.brand)
            }
        } else {
            _brandDuplicateWarning.value = ""
        }
    }
    
    private suspend fun checkBrandDuplicates(brand: String) {
        val existingBrands = repository.getAllBrands().first()
        val normalizedNewBrand = normalizeBrand(brand)
        
        val similarBrands = existingBrands.filter { existingBrand ->
            val normalizedExisting = normalizeBrand(existingBrand)
            isBrandSimilar(normalizedNewBrand, normalizedExisting)
        }
        
        if (similarBrands.isNotEmpty()) {
            _brandDuplicateWarning.value = "⚠️ Похожий бренд уже есть: ${similarBrands.first()}"
        } else {
            _brandDuplicateWarning.value = ""
        }
    }

    fun clearBarcodeInput() {
        _barcodeInput.value = ""
    }
    
    fun showGiftProductDialog() {
        _showGiftProductDialog.value = true
        _giftProductData.value = GiftProductData()
    }
    
    fun hideGiftProductDialog() {
        _showGiftProductDialog.value = false
    }
    
    fun updateGiftProductData(data: GiftProductData) {
        _giftProductData.value = data
    }
    
    fun addGiftProduct() {
        val data = _giftProductData.value
        if (data.productId == null) {
            _searchResult.value = "❌ Выберите товар из списка"
            return
        }
        if (data.purchasePrice < 0 || data.purchasePrice > 9999) {
            _searchResult.value = "❌ Цена закупки должна быть от 0 до 9999 BYN"
            return
        }
        
        viewModelScope.launch {
            val product = repository.getProductById(data.productId)
            if (product == null) {
                _searchResult.value = "❌ Товар не найден"
                return@launch
            }
            
            try {
                val updatedProduct = product.copy(
                    purchasePrice = data.purchasePrice,
                    stock = product.stock + 1
                )
                repository.updateProduct(updatedProduct)
                _searchResult.value = "✅ Добавлен подарочный товар:\n${product.brand} - ${product.flavor}\n💰 Цена закупки: ${"%.2f".format(data.purchasePrice)} BYN"
                _showGiftProductDialog.value = false
                loadBrands()
            } catch (e: Exception) {
                _searchResult.value = "❌ Ошибка: ${e.message}"
            }
        }
    }
    
    private val DEBOUNCE_DELAY = 500L // 500ms задержка
}

data class GiftProductData(
    val productId: Int? = null,
    val purchasePrice: Double = 0.0
)

data class NewProductData(
    val brand: String = "",
    val flavor: String = "",
    val barcode: String = "",
    val purchasePrice: String = "",
    val retailPrice: String = "",
    val category: String = "liquid",
    val strength: String = "",
    val specification: String = "",
    val isGift: Boolean = false, // Подарочный товар
    val quantity: Int = 1 // Количество позиций для добавления
)