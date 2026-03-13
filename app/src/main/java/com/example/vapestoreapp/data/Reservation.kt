package com.example.vapestoreapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val productId: Int,
    val quantity: Int,
    val reservationDate: Long = System.currentTimeMillis(),
    val expirationDate: Long,
    val isSold: Boolean = false
)
