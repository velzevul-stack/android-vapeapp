package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AcceptViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcceptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AcceptViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}