package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private lateinit var repository: Repository

    fun initRepository(context: Context) {
        repository = Repository(context)
    }

    // Пока без Excel импорта - добавим позже
    fun addTestData() {
        viewModelScope.launch {
            // Тестовые данные для проверки
            repository.insertProduct(
                Product(
                    brand = "PODONKI & MALAYSIAN ARCADE 50 mg",
                    flavor = "КЛУБНИКА АНАНАСОВЫЕ КОЛЬЦА",
                    purchasePrice = 8.0,
                    retailPrice = 15.0,
                    stock = 1
                )
            )

            repository.insertProduct(
                Product(
                    brand = "PODONKI CRITICAL 50 mg",
                    flavor = "АПЕЛЬСИНОВОЕ ДРАЖЕ",
                    purchasePrice = 8.0,
                    retailPrice = 15.0,
                    stock = 2
                )
            )
        }
    }
}