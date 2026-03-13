package com.example.vapestoreapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val date: Long = System.currentTimeMillis(),
    val comment: String? = null,
    val discount: Double = 0.0,
    val revenue: Double,
    val profit: Double,
    val quantity: Int = 1,
    val paymentMethod: String = "cash",
    val cashAmount: Double? = null,
    val cardAmount: Double? = null,
    val cardId: Int? = null,
    val isCancelled: Boolean = false,
    val originalSaleId: Int? = null,
    val sourceType: String? = null, // "debt", "reservation", null для обычной продажи
    val sourceId: Int? = null // ID долга или резерва
)