package com.example.vapestoreapp.data

import android.content.Context
import com.example.vapestoreapp.utils.ExcelImporter
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Элемент долга для парсинга JSON */
data class DebtProductItem(
    @SerializedName("productId") val productId: Int,
    @SerializedName("quantity") val quantity: Int
)

class Repository(context: Context) {
    private val db = AppDatabase.getInstance(context.applicationContext)

    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val debtDao = db.debtDao()
    private val reservationDao = db.reservationDao()
    private val paymentCardDao = db.paymentCardDao()
    private val appContext = context

    private fun nowTickerFlow(periodMs: Long = 60_000L): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }.conflate()

    // ========== PRODUCT OPERATIONS ==========

    fun getAllProducts(): Flow<List<Product>> = productDao.getAll()

    fun getAllProductsWithStock(): Flow<List<Product>> = productDao.getAllWithStock()

    suspend fun getProductByBarcode(barcode: String): Product? =
        productDao.getByBarcode(barcode)

    suspend fun searchProducts(query: String): List<Product> =
        productDao.search("%$query%")

    suspend fun insertProduct(product: Product) {
        // Получаем максимальный orderIndex для категории
        val maxOrder = productDao.getMaxOrderIndexInCategory(product.category) ?: 0
        val productWithOrder = product.copy(orderIndex = maxOrder + 1)
        productDao.insert(productWithOrder)
    }

    suspend fun updateProduct(product: Product) = productDao.update(product)

    suspend fun deleteProduct(id: Int) = productDao.delete(id)

    fun getBrandsWithStock(): Flow<List<String>> = productDao.getBrandsWithStock()

    fun getAllBrands(): Flow<List<String>> = productDao.getAllBrands()

    suspend fun getFlavorsByBrand(brand: String): List<Product> {
        return productDao.getFlavorsByBrand(brand)
    }

    suspend fun getProductsByBrand(brand: String): List<Product> {
        return productDao.getProductsByBrand(brand)
    }

    // НОВЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ПОРЯДКОМ
    suspend fun updateProductOrder(productId: Int, newOrder: Int) {
        productDao.updateOrder(productId, newOrder)
    }

    suspend fun reorderProducts(category: String, orderedIds: List<Int>) {
        withContext(Dispatchers.IO) {
            orderedIds.forEachIndexed { index, productId ->
                productDao.updateOrder(productId, index + 1)
            }
        }
    }

    suspend fun moveProduct(fromPosition: Int, toPosition: Int, category: String, brand: String?) {
        val products = if (brand != null) {
            productDao.getProductsByBrand(brand).filter { it.category == category }
        } else {
            productDao.getAll().first().filter { it.category == category }
        }.sortedBy { it.orderIndex }

        if (fromPosition < 0 || fromPosition >= products.size ||
            toPosition < 0 || toPosition >= products.size) return

        val movedProduct = products[fromPosition]
        val newOrderList = mutableListOf<Product>()

        when {
            fromPosition < toPosition -> {
                // Двигаем вниз
                for (i in 0 until fromPosition) newOrderList.add(products[i])
                for (i in fromPosition + 1..toPosition) newOrderList.add(products[i])
                newOrderList.add(movedProduct)
                for (i in toPosition + 1 until products.size) newOrderList.add(products[i])
            }
            fromPosition > toPosition -> {
                // Двигаем вверх
                for (i in 0 until toPosition) newOrderList.add(products[i])
                newOrderList.add(movedProduct)
                for (i in toPosition until fromPosition) newOrderList.add(products[i])
                for (i in fromPosition + 1 until products.size) newOrderList.add(products[i])
            }
            else -> return
        }

        // Обновляем порядок в базе данных
        newOrderList.forEachIndexed { index, product ->
            productDao.updateOrder(product.id, index + 1)
        }
    }


    // ========== SALE OPERATIONS ==========

    suspend fun insertSale(sale: Sale) = saleDao.insert(sale)

    fun getAllSales(): Flow<List<Sale>> = saleDao.getAll()

    fun getActiveSales(): Flow<List<Sale>> = saleDao.getAllActiveSales()

    fun getSalesInPeriod(start: Long, end: Long): Flow<List<Sale>> =
        saleDao.getSalesInPeriod(start, end)

    fun getTotalProfit(start: Long, end: Long): Flow<Double?> =
        saleDao.getTotalProfit(start, end)

    // ========== HELPER METHODS ==========

    suspend fun increaseStock(productId: Int) {
        val product = productDao.getById(productId)
        product?.let {
            productDao.update(it.copy(stock = it.stock + 1))
        }
    }

    suspend fun getProductById(id: Int): Product? = productDao.getById(id)

    suspend fun getSaleById(saleId: Int): Sale? = saleDao.getById(saleId)

    suspend fun decreaseStock(productId: Int) {
        val product = productDao.getById(productId)
        product?.let {
            if (it.stock > 0) {
                productDao.update(it.copy(stock = it.stock - 1))
            }
        }
    }

    fun getBrandsByCategory(category: String): Flow<List<String>> {
        return productDao.getBrandsByCategory(category)
    }

    // ========== УПРАВЛЕНИЕ ПРОДАЖАМИ ==========

    suspend fun updateSale(sale: Sale) = saleDao.update(sale)

    suspend fun cancelSale(saleId: Int) = saleDao.cancelSale(saleId)

    suspend fun correctSale(
        originalSaleId: Int,
        newProductId: Int? = null,
        newQuantity: Int? = null,
        newDiscount: Double? = null,
        newPaymentMethod: String? = null,
        newDate: Long? = null,
        comment: String? = null
    ): Boolean {
        val originalSale = saleDao.getById(originalSaleId) ?: return false
        val product = newProductId?.let { productDao.getById(it) } ?:
        productDao.getById(originalSale.productId)

        if (product == null) return false

        // Возвращаем товар из оригинальной продажи на склад
        productDao.update(product.copy(stock = product.stock + originalSale.quantity))

        // Создаем новую продажу с исправлениями
        val finalQuantity = newQuantity ?: originalSale.quantity
        val finalDiscount = newDiscount ?: originalSale.discount
        val finalPaymentMethod = newPaymentMethod ?: originalSale.paymentMethod

        val finalDate = newDate ?: originalSale.date
        val finalCashAmount = if (finalPaymentMethod == "split") originalSale.cashAmount else null
        val finalCardAmount = if (finalPaymentMethod == "split") originalSale.cardAmount else null
        val finalCardId = if (finalPaymentMethod == "card" || finalPaymentMethod == "split") originalSale.cardId else null
        val correctedSale = Sale(
            productId = newProductId ?: originalSale.productId,
            date = finalDate,
            comment = comment ?: "Исправление продажи #$originalSaleId",
            discount = finalDiscount,
            revenue = product.retailPrice * finalQuantity - finalDiscount,
            profit = (product.retailPrice - product.purchasePrice) * finalQuantity - finalDiscount,
            quantity = finalQuantity,
            paymentMethod = finalPaymentMethod,
            cashAmount = finalCashAmount,
            cardAmount = finalCardAmount,
            cardId = finalCardId,
            originalSaleId = originalSaleId
        )

        // Уменьшаем количество товара на складе для новой продажи
        productDao.update(product.copy(stock = product.stock - finalQuantity))

        // Отменяем оригинальную продажу
        saleDao.cancelSale(originalSaleId)

        // Сохраняем исправленную продажу
        saleDao.insert(correctedSale)

        return true
    }

    suspend fun deleteSale(saleId: Int): Boolean {
        val sale = saleDao.getById(saleId) ?: return false
        val product = productDao.getById(sale.productId) ?: return false

        // Возвращаем товар на склад
        productDao.update(product.copy(stock = product.stock + sale.quantity))

        // Отменяем продажу
        saleDao.cancelSale(saleId)

        return true
    }

    // ========== ИМПОРТ ДАННЫХ ИЗ EXCEL ==========

    suspend fun importInitialData(): List<Product> = withContext(Dispatchers.IO) {
        val existingCount = productDao.getAll().first().size

        if (existingCount > 0) {
            return@withContext emptyList()
        }

        val importedProducts = ExcelImporter.importFromExcel(appContext)

        importedProducts.forEach { product ->
            productDao.insert(product)
        }

        return@withContext importedProducts
    }

    fun getFilteredProducts(category: String?, brand: String?): Flow<List<Product>> {
        return productDao.getFilteredProducts(category, brand)
    }

    // ========== PAYMENT CARDS ==========

    fun getActivePaymentCards(): Flow<List<PaymentCard>> = paymentCardDao.getActiveCards()

    fun getAllPaymentCards(): Flow<List<PaymentCard>> = paymentCardDao.getAllCards()

    suspend fun upsertPaymentCard(label: String): PaymentCard = withContext(Dispatchers.IO) {
        val normalized = label.trim()
        val existing = paymentCardDao.getByLabel(normalized)
        if (existing != null) {
            if (existing.isArchived) {
                val updated = existing.copy(isArchived = false)
                paymentCardDao.update(updated)
                return@withContext updated
            }
            return@withContext existing
        }
        val id = paymentCardDao.insert(PaymentCard(label = normalized)).toInt()
        return@withContext PaymentCard(id = id, label = normalized)
    }

    suspend fun archivePaymentCard(cardId: Int, archived: Boolean) = withContext(Dispatchers.IO) {
        val existing = paymentCardDao.getById(cardId) ?: return@withContext
        paymentCardDao.update(existing.copy(isArchived = archived))
    }

    suspend fun updateProducts(products: List<Product>) {
        productDao.updateProducts(products)
    }

    // ========== ДОЛГИ ==========

    fun getAllActiveDebts(): Flow<List<Debt>> = debtDao.getAllActiveDebts()

    suspend fun insertDebt(debt: Debt) = debtDao.insert(debt)

    suspend fun updateDebt(debt: Debt) = debtDao.update(debt)

    suspend fun getDebtById(id: Int): Debt? = debtDao.getById(id)

    // ========== РЕЗЕРВЫ ==========

    fun getAllActiveReservations(): Flow<List<Reservation>> {
        return reservationDao.getAllActiveReservations()
            .combine(nowTickerFlow()) { reservations, now ->
                reservations.filter { it.expirationDate > now }
            }
    }

    suspend fun insertReservation(reservation: Reservation) = reservationDao.insert(reservation)

    suspend fun updateReservation(reservation: Reservation) = reservationDao.update(reservation)

    suspend fun getReservationsForProduct(productId: Int): List<Reservation> {
        val currentTime = System.currentTimeMillis()
        return reservationDao.getReservationsForProduct(productId)
            .filter { it.expirationDate > currentTime }
    }

    /** Возвращает Map: productId -> зарезервированное количество (только активные, не истекшие) */
    suspend fun getReservedQuantityByProduct(): Map<Int, Int> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        reservationDao.getAllActiveReservations().first()
            .filter { it.expirationDate > currentTime }
            .groupBy { it.productId }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }
    }

    /** Реактивное количество резервов: productId -> qty (активные, не истекшие) */
    fun getReservedQuantityByProductFlow(): Flow<Map<Int, Int>> {
        val reservationsFlow = reservationDao.getAllActiveReservations()
            .combine(nowTickerFlow()) { reservations, now ->
                reservations.filter { it.expirationDate > now }
            }
            .map { activeReservations ->
                activeReservations
                    .groupBy { it.productId }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }
            }

        val undeductedDebtsFlow = debtDao.getAllActiveDebts().map { debts ->
            val gson = com.google.gson.Gson()
            val qty = mutableMapOf<Int, Int>()
            debts.filter { !it.stockDeducted }.forEach { debt ->
                try {
                    val items = gson.fromJson(debt.products, Array<DebtProductItem>::class.java).toList()
                    items.forEach { item ->
                        qty[item.productId] = (qty[item.productId] ?: 0) + item.quantity
                    }
                } catch (_: Exception) {
                    // Игнорируем битые строки; лучше не ломать обновления UI
                }
            }
            qty
        }

        return reservationsFlow.combine(undeductedDebtsFlow) { reservedByReservation, reservedByDebt ->
            if (reservedByDebt.isEmpty()) return@combine reservedByReservation
            val merged = reservedByReservation.toMutableMap()
            reservedByDebt.forEach { (productId, debtQty) ->
                merged[productId] = (merged[productId] ?: 0) + debtQty
            }
            merged
        }
    }

    /** Автоматически возвращает товары на склад при истечении срока резерва */
    suspend fun returnExpiredReservations(): Int = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expiredReservations = reservationDao.getExpiredReservations(currentTime)
        var returnedCount = 0
        
        expiredReservations.forEach { reservation ->
            // Помечаем резерв как проданный (чтобы он больше не учитывался)
            // Товар остается на складе, так как он никогда не списывался
            reservationDao.update(reservation.copy(isSold = true))
            returnedCount++
        }
        
        returnedCount
    }

    /** Оплата долга: создаёт продажи, списывает со склада, помечает долг оплаченным */
    suspend fun payDebt(debtId: Int): Boolean = withContext(Dispatchers.IO) {
        val debt = debtDao.getById(debtId) ?: return@withContext false
        if (debt.isPaid) return@withContext false
        try {
            val items = com.google.gson.Gson().fromJson(
                debt.products,
                Array<DebtProductItem>::class.java
            ).toList()
            for (item in items) {
                val product = productDao.getById(item.productId) ?: continue
                val shouldDeductStock = !debt.stockDeducted
                if (shouldDeductStock && product.stock < item.quantity) return@withContext false
                val revenue = product.retailPrice * item.quantity
                val profit = (product.retailPrice - product.purchasePrice) * item.quantity
                saleDao.insert(
                    Sale(
                        productId = product.id,
                        date = System.currentTimeMillis(),
                        comment = "Оплата долга #${debt.id}",
                        revenue = revenue,
                        profit = profit,
                        quantity = item.quantity,
                        sourceType = "debt",
                        sourceId = debt.id
                    )
                )
                // Если склад уже был списан при создании долга — повторно не списываем
                if (shouldDeductStock) {
                    productDao.update(product.copy(stock = product.stock - item.quantity))
                }
            }
            debtDao.update(debt.copy(isPaid = true))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getReservationById(id: Int): Reservation? = reservationDao.getById(id)

    /** Продажа резерва: создаёт продажу, списывает со склада, помечает резерв проданным */
    suspend fun sellReservation(reservationId: Int): Boolean = withContext(Dispatchers.IO) {
        val reservation = reservationDao.getById(reservationId) ?: return@withContext false
        val product = productDao.getById(reservation.productId) ?: return@withContext false
        // Проверяем наличие товара на складе (учитывая резервы)
        val reservedOther = getReservationsForProduct(product.id)
            .filter { it.id != reservation.id }
            .sumOf { it.quantity }
        val availableStock = product.stock - reservedOther
        if (availableStock < reservation.quantity) return@withContext false
        try {
            val revenue = product.retailPrice * reservation.quantity
            val profit = (product.retailPrice - product.purchasePrice) * reservation.quantity
            saleDao.insert(
                Sale(
                    productId = product.id,
                    date = System.currentTimeMillis(),
                    comment = "Продажа резерва #${reservation.id}",
                    revenue = revenue,
                    profit = profit,
                    quantity = reservation.quantity,
                    sourceType = "reservation",
                    sourceId = reservation.id
                )
            )
            // Товар списывается со склада только при продаже резерва
            productDao.update(product.copy(stock = product.stock - reservation.quantity))
            reservationDao.update(reservation.copy(isSold = true))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ========== МАССОВОЕ ИЗМЕНЕНИЕ ЦЕН ==========

    suspend fun updatePurchasePriceForBrand(brand: String, newPrice: Double) {
        val products = productDao.getProductsByBrand(brand)
        products.forEach { product ->
            productDao.update(product.copy(purchasePrice = newPrice))
        }
    }

    // ========== ЭКСПОРТ/ИМПОРТ БД ==========

    suspend fun importBackupData(products: List<Product>, sales: List<Sale>, cards: List<PaymentCard> = emptyList()) {
        withContext(Dispatchers.IO) {
            saleDao.deleteAll()
            productDao.deleteAll()
            paymentCardDao.deleteAll()
            products.forEach { productDao.insert(it) }
            cards.forEach { paymentCardDao.insert(it) }
            sales.forEach { saleDao.insert(it) }
        }
    }

    // ========== ОЧИСТКА СКЛАДА ==========

    suspend fun clearAllStock() = withContext(Dispatchers.IO) {
        productDao.clearAllStock()
    }
}