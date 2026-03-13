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
fun EditSaleDialog(
    viewModel: SalesManagementViewModel,
    sale: Sale
) {
    // ВСЕ состояния через collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val editQuantity by viewModel.editQuantity.collectAsState()
    val editDiscount by viewModel.editDiscount.collectAsState()
    val editPaymentMethod by viewModel.editPaymentMethod.collectAsState()
    val editComment by viewModel.editComment.collectAsState()
    val editDate by viewModel.editDate.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filteredProducts = remember(searchQuery, allProducts) {
        allProducts.filter {
            it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.flavor.contains(searchQuery, ignoreCase = true)
        }.take(10)
    }

    // Проверяем selectedProduct через let для безопасного доступа
    val selectedProductStock = selectedProduct?.stock ?: 0

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                viewModel.closeEditDialog()
            }
        },
        title = {
            Text("✏️ Редактирование продажи #${sale.id}", fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Выбор товара
                Text("Товар:", style = MaterialTheme.typography.labelMedium)

                // Поиск товара
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск товара") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )

                LazyColumn(
                    modifier = Modifier.height(150.dp)
                ) {
                    items(filteredProducts) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            onClick = { viewModel.selectProductForEdit(product) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedProduct?.id == product.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    product.displayName(),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp
                                )
                                Badge(
                                    containerColor = if (product.stock > 0)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                ) {
                                    Text("${product.stock} шт.", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Выбранный товар
                selectedProduct?.let { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Выбран: ${product.brand}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                                Text(
                                    product.flavor,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Доступно: ${product.stock} шт.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Количество
                Text("Количество:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.updateEditQuantity(editQuantity - 1)
                        },
                        enabled = editQuantity > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                    }

                    Text("$editQuantity шт.", style = MaterialTheme.typography.bodyMedium)

                    IconButton(
                        onClick = {
                            viewModel.updateEditQuantity(editQuantity + 1)
                        },
                        enabled = selectedProductStock > editQuantity
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить")
                    }
                }

                // Дата и время
                Text("Дата и время:", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (editDate > 0) {
                            SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(editDate))
                        } else "Выбрать дату"
                    )
                }
                if (showDatePicker) {
                    val datePickerState = androidx.compose.material3.rememberDatePickerState(
                        initialSelectedDateMillis = if (editDate > 0) editDate else System.currentTimeMillis()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        viewModel.updateEditDate(it)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Отмена")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Метод оплаты
                Text("Метод оплаты:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = editPaymentMethod == "cash",
                        onClick = { viewModel.updatePaymentMethod("cash") },
                        label = { Text("💵 Наличные") }
                    )
                    FilterChip(
                        selected = editPaymentMethod == "card",
                        onClick = { viewModel.updatePaymentMethod("card") },
                        label = { Text("💳 Карта") }
                    )
                }

                // Скидка
                Text("Скидка (BYN):", style = MaterialTheme.typography.labelMedium)
                val maxEditDiscount = remember(selectedProduct, editQuantity) {
                    viewModel.getMaxEditDiscount()
                }
                val editDiscountText = remember { mutableStateOf("") }
                LaunchedEffect(editDiscount) {
                    editDiscountText.value = editDiscount.toString()
                }
                val isEditDiscountValid = remember(editDiscountText.value, maxEditDiscount) {
                    val value = editDiscountText.value.toDoubleOrNull() ?: 0.0
                    value in 0.0..maxEditDiscount
                }
                OutlinedTextField(
                    value = editDiscountText.value,
                    onValueChange = {
                        editDiscountText.value = it
                        viewModel.updateEditDiscount(it.toDoubleOrNull() ?: 0.0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("BYN ") },
                    isError = editDiscountText.value.isNotEmpty() && !isEditDiscountValid,
                    supportingText = {
                        if (editDiscountText.value.isNotEmpty() && !isEditDiscountValid) {
                            Text(
                                "Максимальная скидка: ${"%.2f".format(maxEditDiscount)} BYN",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (maxEditDiscount > 0) {
                            Text(
                                "Макс: ${"%.2f".format(maxEditDiscount)} BYN",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Комментарий
                Text("Комментарий:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = editComment,
                    onValueChange = { viewModel.updateEditComment(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    viewModel.saveCorrection()

                    // Используем coroutineScope вместо LaunchedEffect
                    coroutineScope.launch {
                        delay(2000)
                        isSaving = false
                    }
                },
                enabled = selectedProduct != null && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Сохранение..." else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isSaving) {
                        viewModel.closeEditDialog()
                    }
                },
                enabled = !isSaving
            ) {
                Text("Отмена")
            }
        }
    )
}
