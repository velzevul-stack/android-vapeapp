package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DebtsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
