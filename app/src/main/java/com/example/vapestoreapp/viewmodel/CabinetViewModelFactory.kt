package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CabinetViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CabinetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CabinetViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}