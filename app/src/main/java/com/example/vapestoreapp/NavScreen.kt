package com.example.vapestoreapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreen(val route: String, val title: String, val icon: ImageVector, val isHome: Boolean = false) {
    object Accept : NavScreen("accept", "Приемка", Icons.Filled.AddCircle)
    object Sell : NavScreen("sell", "Продажа", Icons.Filled.ShoppingCart)
    object Home : NavScreen("home", "Главная", Icons.Filled.Home, isHome = true)
    object Cabinet : NavScreen("cabinet", "Кабинет", Icons.Filled.Person)
    object Management : NavScreen("management", "Управление", Icons.Filled.Settings)
}
