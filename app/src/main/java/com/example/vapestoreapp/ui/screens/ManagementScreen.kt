package com.example.vapestoreapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapestoreapp.ui.components.ScreenScaffold
import com.example.vapestoreapp.viewmodel.ManagementScreen as ManagementScreenEnum
import com.example.vapestoreapp.viewmodel.ManagementViewModel
import com.example.vapestoreapp.viewmodel.CabinetViewModel
import com.example.vapestoreapp.viewmodel.CabinetViewModelFactory

@Composable
fun ManagementScreen(
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ManagementViewModel = viewModel()
    val cabinetViewModel: CabinetViewModel = viewModel(factory = CabinetViewModelFactory(context))

    val currentScreen by viewModel.currentScreen.collectAsState()

    ScreenScaffold(
        title = "Управление",
        subtitle = "Долги, бронь, продажи и настройки",
        leadingIcon = Icons.Default.Settings
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
                        selected = currentScreen == ManagementScreenEnum.DEBTS,
                        onClick = { viewModel.navigateTo(ManagementScreenEnum.DEBTS) },
                        text = { Text("Долги") }
                    )
                    Tab(
                        selected = currentScreen == ManagementScreenEnum.RESERVATIONS,
                        onClick = { viewModel.navigateTo(ManagementScreenEnum.RESERVATIONS) },
                        text = { Text("Бронь") }
                    )
                    Tab(
                        selected = currentScreen == ManagementScreenEnum.SALES_MANAGEMENT,
                        onClick = { viewModel.navigateTo(ManagementScreenEnum.SALES_MANAGEMENT) },
                        text = { Text("Продажи") }
                    )
                    Tab(
                        selected = currentScreen == ManagementScreenEnum.SETTINGS,
                        onClick = { viewModel.navigateTo(ManagementScreenEnum.SETTINGS) },
                        text = { Text("Настройки") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    ManagementScreenEnum.DEBTS -> DebtsScreen()
                    ManagementScreenEnum.RESERVATIONS -> ReservationsScreen()
                    ManagementScreenEnum.SALES_MANAGEMENT -> SalesManagementScreen()
                    ManagementScreenEnum.SETTINGS -> SettingsScreen(
                        viewModel = cabinetViewModel,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange
                    )
                }
            }
        }
    }
}
