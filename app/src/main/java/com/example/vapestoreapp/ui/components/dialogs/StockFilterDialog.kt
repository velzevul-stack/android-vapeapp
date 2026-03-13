package com.example.vapestoreapp.ui.components.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vapestoreapp.components.DragDropProductList
import com.example.vapestoreapp.data.Debt
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.data.Repository
import com.example.vapestoreapp.data.Reservation
import com.example.vapestoreapp.data.Sale
import com.example.vapestoreapp.screens.CameraScanner
import com.example.vapestoreapp.ui.theme.VapeStoreAppTheme
import com.example.vapestoreapp.utils.displayName
import com.example.vapestoreapp.utils.displaySubtitle
import com.example.vapestoreapp.viewmodel.AcceptViewModel
import com.example.vapestoreapp.viewmodel.AcceptViewModelFactory
import com.example.vapestoreapp.viewmodel.CabinetViewModel
import com.example.vapestoreapp.viewmodel.CabinetViewModelFactory
import com.example.vapestoreapp.viewmodel.DebtsViewModel
import com.example.vapestoreapp.viewmodel.DebtsViewModelFactory
import com.example.vapestoreapp.viewmodel.GiftProductData
import com.example.vapestoreapp.viewmodel.ManagementScreen
import com.example.vapestoreapp.viewmodel.ManagementViewModel
import com.example.vapestoreapp.viewmodel.NewProductData
import com.example.vapestoreapp.viewmodel.Period
import com.example.vapestoreapp.viewmodel.ReservationsViewModel
import com.example.vapestoreapp.viewmodel.ReservationsViewModelFactory
import com.example.vapestoreapp.viewmodel.SaleDisplay
import com.example.vapestoreapp.viewmodel.SalesByDay
import com.example.vapestoreapp.viewmodel.SalesManagementViewModel
import com.example.vapestoreapp.viewmodel.SalesManagementViewModelFactory
import com.example.vapestoreapp.viewmodel.SellViewModel
import com.example.vapestoreapp.viewmodel.SellViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.vapestoreapp.ui.screens.*
import com.example.vapestoreapp.ui.components.*
import com.example.vapestoreapp.ui.components.dialogs.*
import com.example.vapestoreapp.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockFilterDialog(
    viewModel: CabinetViewModel,
    currentFilters: com.example.vapestoreapp.viewmodel.StockFilters,
    onDismiss: () -> Unit,
    onApply: (com.example.vapestoreapp.viewmodel.StockFilters) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategories by remember { mutableStateOf(currentFilters.selectedCategories) }
    var selectedBrands by remember { mutableStateOf(currentFilters.selectedBrands) }
    var selectedMgValues by remember { mutableStateOf(currentFilters.selectedMgValues) }
    var mgManualInput by remember { mutableStateOf("") }
    var minPriceText by remember { mutableStateOf(currentFilters.minPrice?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(currentFilters.maxPrice?.toString() ?: "") }
    var hasStock by remember { mutableStateOf(currentFilters.hasStock) }
    
    // Загружаем доступные значения
    var availableCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableBrands by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableMgValues by remember { mutableStateOf<List<String>>(emptyList()) }
    var priceRange by remember { mutableStateOf<Pair<Double, Double>>(Pair(0.0, 0.0)) }
    
    LaunchedEffect(Unit) {
        availableCategories = viewModel.getAvailableCategories()
        availableBrands = viewModel.getAvailableBrands()
        availableMgValues = viewModel.getAvailableMgValues()
        priceRange = viewModel.getPriceRange()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍 Фильтры прайс-листа")
                if (!currentFilters.isEmpty()) {
                    TextButton(onClick = onReset) {
                        Text("Сбросить", fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Категории
                Text("Категории:", style = MaterialTheme.typography.labelMedium)
                val categoryMap = mapOf(
                    "liquid" to "🍬 Жидкость",
                    "disposable" to "🚬 Одноразка",
                    "consumable" to "⚙️ Расходник",
                    "vape" to "🔋 Вейп",
                    "snus" to "🫀 Снюс"
                )
                availableCategories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = category in selectedCategories,
                            onCheckedChange = { checked ->
                                selectedCategories = if (checked) {
                                    selectedCategories + setOf(category)
                                } else {
                                    selectedCategories - setOf(category)
                                }
                            }
                        )
                        Text(
                            categoryMap[category] ?: category,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Бренды
                Text("Бренды:", style = MaterialTheme.typography.labelMedium)
                var brandSearchText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = brandSearchText,
                    onValueChange = { brandSearchText = it },
                    label = { Text("Поиск бренда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                val filteredBrands = availableBrands.filter {
                    it.contains(brandSearchText, ignoreCase = true)
                }
                filteredBrands.forEach { brand ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = brand in selectedBrands,
                            onCheckedChange = { checked ->
                                selectedBrands = if (checked) {
                                    selectedBrands + setOf(brand)
                                } else {
                                    selectedBrands - setOf(brand)
                                }
                            }
                        )
                        Text(brand, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                HorizontalDivider()
                
                // MG значения
                Text("Крепость (mg):", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableMgValues.forEach { mg ->
                        FilterChip(
                            selected = mg in selectedMgValues,
                            onClick = {
                                selectedMgValues = if (mg in selectedMgValues) {
                                    selectedMgValues - setOf(mg)
                                } else {
                                    selectedMgValues + setOf(mg)
                                }
                            },
                            label = { Text("${mg}mg") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mgManualInput,
                        onValueChange = { mgManualInput = it.take(8) },
                        label = { Text("Добавить mg вручную") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Например 50") }
                    )
                    Button(
                        onClick = {
                            val normalized = mgManualInput
                                .trim()
                                .lowercase(Locale.getDefault())
                                .replace("mg", "")
                                .filter { it.isDigit() }
                                .trimStart('0')
                                .ifBlank { "0" }
                            if (normalized != "0") {
                                selectedMgValues = selectedMgValues + setOf(normalized)
                                if (normalized !in availableMgValues) {
                                    availableMgValues = (availableMgValues + normalized)
                                        .distinct()
                                        .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                }
                                mgManualInput = ""
                            }
                        }
                    ) {
                        Text("Добавить")
                    }
                }
                
                HorizontalDivider()
                
                // Цены
                Text("Цена (BYN):", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it },
                        label = { Text("От") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("BYN ") }
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it },
                        label = { Text("До") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("BYN ") }
                    )
                }
                if (priceRange.first > 0 || priceRange.second > 0) {
                    Text(
                        "Диапазон: ${"%.2f".format(priceRange.first)} - ${"%.2f".format(priceRange.second)} BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // Только с остатком
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Только товары с остатком")
                    Switch(
                        checked = hasStock,
                        onCheckedChange = { hasStock = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filters = com.example.vapestoreapp.viewmodel.StockFilters(
                        selectedCategories = selectedCategories,
                        selectedBrands = selectedBrands,
                        selectedMgValues = selectedMgValues,
                        minPrice = minPriceText.toDoubleOrNull(),
                        maxPrice = maxPriceText.toDoubleOrNull(),
                        hasStock = hasStock
                    )
                    onApply(filters)
                    onDismiss()
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
