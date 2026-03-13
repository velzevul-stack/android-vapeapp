package com.example.vapestoreapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.ui.theme.VapeStoreAppTheme
import com.example.vapestoreapp.ui.screens.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }
    
    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Убираем непрозрачный фон окна для клавиатуры в edge-to-edge режиме
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        Log.d(TAG, "🏪 Vape Store App запускается...")

        setContent {
            val themeManager = remember { com.example.vapestoreapp.utils.ThemeManager(this@MainActivity) }
            var isDarkMode by remember { mutableStateOf(themeManager.isDarkMode()) }
            val onDarkModeChange: (Boolean) -> Unit = {
                themeManager.setDarkMode(it)
                isDarkMode = it
            }
            
            VapeStoreAppTheme(darkTheme = isDarkMode) {
                AppNavigation(isDarkMode = isDarkMode, onDarkModeChange = onDarkModeChange)
            }
        }

        // Запускаем импорт в фоне
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔄 Начинаю импорт данных...")
                val repository = Repository(this@MainActivity)
                val importedProducts = repository.importInitialData()
                Log.d(TAG, "✅ Импорт завершен. Добавлено ${importedProducts.size} товаров")

                // Для отладки: показываем что импортировалось
                importedProducts.forEachIndexed { index, product ->
                    Log.d(TAG, "${index + 1}. ${product.brand} - ${product.flavor}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при импорте данных: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun AppNavigation(isDarkMode: Boolean = true, onDarkModeChange: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()
    val items = listOf(
        NavScreen.Accept,
        NavScreen.Sell,
        NavScreen.Cabinet,
        NavScreen.Management
    )

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavScreen.Sell.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavScreen.Accept.route) { AcceptScreen() }
            composable(NavScreen.Sell.route) { SellScreen() }
            composable(NavScreen.Cabinet.route) {
                CabinetScreen(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange
                )
            }
            composable(NavScreen.Management.route) {
                ManagementScreen(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange
                )
            }
        }
    }
}

@Preview(showBackground = true)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun AppPreview() {
    VapeStoreAppTheme {
        AcceptScreen()
    }
}
