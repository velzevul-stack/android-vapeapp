package com.example.vapestoreapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val brand: String,
    val flavor: String,
    val barcode: String? = null,
    val purchasePrice: Double,
    val retailPrice: Double,
    val stock: Int = 0,
    val category: String = "liquid",
    val strength: String = "",
    val specification: String = "",
    val orderIndex: Int = 0
)