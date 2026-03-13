package com.example.vapestoreapp.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ManagementViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow(ManagementScreen.DEBTS)
    val currentScreen: StateFlow<ManagementScreen> = _currentScreen

    fun navigateTo(screen: ManagementScreen) {
        _currentScreen.value = screen
    }
}

enum class ManagementScreen {
    DEBTS,
    RESERVATIONS,
    SALES_MANAGEMENT,
    SETTINGS
}
