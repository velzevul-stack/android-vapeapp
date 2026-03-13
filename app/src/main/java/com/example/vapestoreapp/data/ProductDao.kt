package com.example.vapestoreapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // УПРОЩАЕМ: сортируем ТОЛЬКО по orderIndex для раздела "Товары"
    @Query("SELECT * FROM products ORDER BY " +
            "CASE category " +
            "WHEN 'liquid' THEN 1 " +
            "WHEN 'disposable' THEN 2 " +
            "WHEN 'consumable' THEN 3 " +
            "WHEN 'vape' THEN 4 " +
            "WHEN 'snus' THEN 5 " +
            "ELSE 6 END, " +
            "orderIndex, brand, flavor")
    fun getAll(): Flow<List<Product>>

    // Для раздела "Товары" с фильтрацией - сортируем ТОЛЬКО по orderIndex
    @Query("SELECT * FROM products WHERE " +
            "(:category IS NULL OR category = :category) AND " +
            "(:brand IS NULL OR brand = :brand) " +
            "ORDER BY orderIndex")
    fun getFilteredProducts(category: String?, brand: String?): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE brand LIKE '%' || :query || '%' OR flavor LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Product>

    @Query("SELECT DISTINCT brand FROM products ORDER BY brand")
    fun getAllBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM products WHERE stock > 0 ORDER BY brand")
    fun getBrandsWithStock(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE brand = :brand ORDER BY orderIndex, flavor")
    suspend fun getProductsByBrand(brand: String): List<Product>

    @Query("SELECT * FROM products WHERE brand = :brand AND stock > 0 ORDER BY orderIndex, flavor")
    suspend fun getFlavorsByBrand(brand: String): List<Product>

    @Insert
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Update
    suspend fun updateProducts(products: List<Product>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT DISTINCT brand FROM products WHERE category = :category ORDER BY brand")
    fun getBrandsByCategory(category: String): Flow<List<String>>

    // НОВЫЕ МЕТОДЫ для управления порядком
    @Query("SELECT MAX(orderIndex) FROM products WHERE category = :category")
    suspend fun getMaxOrderIndexInCategory(category: String): Int?

    // Для склада используем полную сортировку с категориями
    @Query("UPDATE products SET orderIndex = :newOrder WHERE id = :productId")
    suspend fun updateOrder(productId: Int, newOrder: Int)
    @Query("SELECT * FROM products WHERE stock > 0 ORDER BY " +
            "CASE category " +
            "WHEN 'liquid' THEN 1 " +
            "WHEN 'disposable' THEN 2 " +
            "WHEN 'consumable' THEN 3 " +
            "WHEN 'vape' THEN 4 " +
            "WHEN 'snus' THEN 5 " +
            "ELSE 6 END, " +
            "orderIndex, brand, flavor")


    fun getAllWithStock(): Flow<List<Product>>

    // Очистка всего склада (обнуление stock у всех товаров)
    @Query("UPDATE products SET stock = 0")
    suspend fun clearAllStock()
}