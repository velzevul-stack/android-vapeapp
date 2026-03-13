package com.example.vapestoreapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryDisplayName(category: String): String {
    return when (category) {
        "liquid" -> "Жидкость"
        "disposable" -> "Одноразка"
        "consumable" -> "Расходник"
        "vape" -> "Вейп"
        "snus" -> "Снюс"
        else -> category
    }
}

fun categoryIcon(category: String): ImageVector {
    return when (category) {
        "liquid" -> Icons.Filled.WaterDrop
        "disposable" -> Icons.Filled.Bolt
        "consumable" -> Icons.Filled.Build
        "vape" -> Icons.Filled.Devices
        "snus" -> Icons.Filled.LocalOffer
        else -> Icons.Filled.Inventory2
    }
}

