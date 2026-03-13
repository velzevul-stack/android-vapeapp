package com.example.vapestoreapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_cards")
data class PaymentCard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

