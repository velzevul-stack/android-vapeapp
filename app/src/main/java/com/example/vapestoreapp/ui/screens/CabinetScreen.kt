package com.example.vapestoreapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapestoreapp.ui.components.ScreenScaffold
import com.example.vapestoreapp.viewmodel.CabinetScreen as CabinetScreenEnum
import com.example.vapestoreapp.viewmodel.CabinetViewModel
import com.example.vapestoreapp.viewmodel.CabinetViewModelFactory

@Composable
fun CabinetScreen(
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CabinetViewModel = viewModel(factory = CabinetViewModelFactory(context))

    val currentScreen by viewModel.currentScreen.collectAsState()
    val stockText by viewModel.stockText.collectAsState()

    ScreenScaffold(
        title = "Кабинет",
        subtitle = "Склад, продажи и товары",
        leadingIcon = Icons.Default.Apps
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                ScrollableTabRow(
                    selectedTabIndex = currentScreen.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = currentScreen == CabinetScreenEnum.STOCK,
                        onClick = { viewModel.navigateTo(CabinetScreenEnum.STOCK) },
                        text = { Text("Склад") }
                    )
                    Tab(
                        selected = currentScreen == CabinetScreenEnum.SALES,
                        onClick = { viewModel.navigateTo(CabinetScreenEnum.SALES) },
                        text = { Text("Продажи") }
                    )
                    Tab(
                        selected = currentScreen == CabinetScreenEnum.PRODUCTS,
                        onClick = { viewModel.navigateTo(CabinetScreenEnum.PRODUCTS) },
                        text = { Text("Товары") }
                    )
                    Tab(
                        selected = currentScreen == CabinetScreenEnum.SALES_MANAGEMENT,
                        onClick = { viewModel.navigateTo(CabinetScreenEnum.SALES_MANAGEMENT) },
                        text = { Text("Управление продажами") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    CabinetScreenEnum.STOCK -> StockScreen(stockText)
                    CabinetScreenEnum.SALES -> SalesScreen(viewModel)
                    CabinetScreenEnum.PRODUCTS -> ProductsScreen(viewModel)
                    CabinetScreenEnum.SALES_MANAGEMENT -> SalesManagementScreen()
                }
            }
        }
    }
}
