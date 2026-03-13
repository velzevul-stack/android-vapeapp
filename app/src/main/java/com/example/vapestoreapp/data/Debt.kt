package com.example.vapestoreapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val products: String, // JSON строка с массивом Product IDs и количеством
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val isPaid: Boolean = false,
    /** true = склад был уменьшен в момент создания долга */
    val stockDeducted: Boolean = false
)
