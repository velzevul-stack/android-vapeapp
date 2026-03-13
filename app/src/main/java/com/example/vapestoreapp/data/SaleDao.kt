package com.example.vapestoreapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(sale: Sale)

    @Update
    suspend fun update(sale: Sale) // НОВОЕ: для обновления

    @Query("SELECT * FROM sales WHERE isCancelled = 0 ORDER BY date DESC")
    fun getAllActiveSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAll(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getById(saleId: Int): Sale?

    @Query("SELECT * FROM sales WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getSalesInPeriod(start: Long, end: Long): Flow<List<Sale>>

    @Query("SELECT SUM(profit) FROM sales WHERE date BETWEEN :start AND :end AND isCancelled = 0")
    fun getTotalProfit(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(revenue) FROM sales WHERE date BETWEEN :start AND :end AND isCancelled = 0")
    fun getTotalRevenue(start: Long, end: Long): Flow<Double?>

    // НОВЫЕ МЕТОДЫ:
    @Query("UPDATE sales SET isCancelled = 1 WHERE id = :saleId")
    suspend fun cancelSale(saleId: Int)

    @Query("SELECT * FROM sales WHERE productId = :productId AND isCancelled = 0")
    fun getSalesForProduct(productId: Int): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE originalSaleId = :originalId")
    suspend fun getCorrectionsForSale(originalId: Int): List<Sale>

    @Query("DELETE FROM sales")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(sales: List<Sale>)

}