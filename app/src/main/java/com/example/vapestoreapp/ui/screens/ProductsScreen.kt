package com.example.vapestoreapp.ui.screens

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
fun ProductsScreen(viewModel: CabinetViewModel) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val brandsForCategory by viewModel.brandsForCategory.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val dragDropEnabled by viewModel.dragDropEnabled.collectAsState()

    // Добавьте состояния для редактирования брендов
    val showEditBrandDialog by viewModel.showEditBrandDialog.collectAsState()
    val editingBrand by viewModel.editingBrand.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showBulkPriceDialog by viewModel.showBulkPriceDialog.collectAsState()
    val bulkPriceBrands by viewModel.bulkPriceBrands.collectAsState()
    val bulkPriceResult by viewModel.bulkPriceResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Заголовок с кнопками - компактно
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📋 Товары",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.refreshFilteredProducts() },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.openBulkPriceDialog() },
                    modifier = Modifier.height(32.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Цена", fontSize = 12.sp)
                }
                Button(
                    onClick = { viewModel.openAddDialog() },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Диалог массового изменения цены закупки
        if (showBulkPriceDialog) {
            BulkPriceDialog(
                brands = bulkPriceBrands,
                onDismiss = { viewModel.closeBulkPriceDialog() },
                onApply = { brand, newPrice -> viewModel.applyBulkPriceChange(brand, newPrice) },
                result = bulkPriceResult
            )
        }

        // Поиск - улучшенная видимость и размер
        if (!dragDropEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Поиск товара", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Бренд, вкус, спецификация...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Фильтры:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Фильтр по категории
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.let { getCategoryDisplayName(it) } ?: "Все категории",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория", fontSize = 12.sp) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Все категории", fontSize = 13.sp) },
                        onClick = {
                            viewModel.selectCategory(null)
                            categoryExpanded = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(getCategoryDisplayName(category), fontSize = 13.sp) },
                            onClick = {
                                viewModel.selectCategory(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Фильтр по бренду
            var brandExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = brandExpanded,
                onExpandedChange = { brandExpanded = !brandExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBrand ?: "Все бренды",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Бренд", fontSize = 12.sp) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                    },
                    enabled = selectedCategory != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )
                ExposedDropdownMenu(
                    expanded = brandExpanded,
                    onDismissRequest = { brandExpanded = false },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Все бренды", fontSize = 13.sp) },
                        onClick = {
                            viewModel.selectBrand(null)
                            brandExpanded = false
                        }
                    )
                    brandsForCategory.forEach { brand ->
                        DropdownMenuItem(
                            text = { Text(brand, fontSize = 13.sp) },
                            onClick = {
                                viewModel.selectBrand(brand)
                                brandExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Статистика
            Text(
                "Найдено: ${filteredProducts.size} товаров",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Используем компонент с drag & drop
        DragDropProductList(
            viewModel = viewModel,
            products = filteredProducts,
            onProductClick = { product ->
                viewModel.openEditDialog(product)
            },
            onUpdateStock = { productId, newStock ->
                viewModel.updateProductStock(productId, newStock)
            },
            onDeleteClick = { productId ->
                viewModel.deleteProduct(productId)
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Диалог добавления/редактирования товара
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = editingProduct,
            onSave = { product ->
                viewModel.saveProduct(product)
            },
            onDismiss = { viewModel.closeAddEditDialog() }
        )
    }

    // Диалог редактирования бренда
    if (showEditBrandDialog && editingBrand != null) {
        EditBrandDialog(
            viewModel = viewModel,
            oldBrand = editingBrand!!,
            onDismiss = { viewModel.closeEditBrandDialog() }
        )
    }
}
