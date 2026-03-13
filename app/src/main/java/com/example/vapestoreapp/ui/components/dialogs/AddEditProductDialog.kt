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
fun AddEditProductDialog(
    product: Product?,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    // Данные формы
    var brand by remember { mutableStateOf(product?.brand ?: "") }
    var flavor by remember { mutableStateOf(product?.flavor ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var purchasePrice by remember {
        mutableStateOf(product?.purchasePrice?.toString() ?: "")
    }
    var retailPrice by remember {
        mutableStateOf(product?.retailPrice?.toString() ?: "")
    }
    var stock by remember {
        mutableStateOf(product?.stock?.toString() ?: "0")
    }
    var category by remember {
        mutableStateOf(product?.category ?: "liquid")
    }
    var strength by remember { mutableStateOf(product?.strength ?: "") }
    var specification by remember { mutableStateOf(product?.specification ?: "") }

    // Категории с эмодзи
    val categories = listOf(
        "liquid" to "🍬 Жидкость",
        "disposable" to "🚬 Одноразка",
        "consumable" to "⚙️ Расходник",
        "vape" to "🔋 Вейп",
        "snus" to " Снюс"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product == null) "➕ ДОБАВИТЬ ТОВАР"
                else "✏️ РЕДАКТИРОВАТЬ ТОВАР"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Категория
                Text("Категория:", style = MaterialTheme.typography.labelMedium)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { (id, name) ->
                        FilterChip(
                            selected = category == id,
                            onClick = { category = id },
                            label = { Text(name) }
                        )
                    }
                }

                // Бренд
                Text("Бренд:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Введите бренд (можно с эмодзи)") }
                )

                // Крепость только для жидкостей
                if (category == "liquid") {
                    Text("Крепость (mg):", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = strength,
                        onValueChange = { strength = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Например: 20, 35, 50") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Вкус для жидкостей, одноразок и снюса
                if (category == "liquid" || category == "disposable" || category == "snus") {
                    Text("Вкус:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = flavor,
                        onValueChange = { flavor = it },
                        label = {
                            Text(
                                when (category) {
                                    "liquid" -> "Вкус жидкости *"
                                    "disposable" -> "Вкус одноразки *"
                                    "snus" -> "Вкус снюса *"
                                    else -> "Вкус *"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                when (category) {
                                    "liquid" -> "Например: АПЕЛЬСИНОВОЕ ДРАЖЕ"
                                    "disposable" -> "Например: КЛУБНИКА АНАНАС"
                                    "snus" -> "Например: МЯТА"
                                    else -> "Вкус"
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Цвет для вейпов
                if (category == "vape") {
                    Text("Цвет:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = specification,
                        onValueChange = { specification = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Например: PURPLE, PASTEL CRYSTAL") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Для расходников - НИЧЕГО дополнительного (без омов)

                // Штрих-код
                Text("Штрих-код:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("13 цифр") }
                )

                // Цены
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Закупка") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0.00") }
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Розница") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0.00") }
                    )
                }

                // Количество
                Text("Количество на складе:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        val digitsOnly = it.filter { char -> char.isDigit() }
                        stock = digitsOnly
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Валидация
                    if (brand.isBlank()) {
                        return@Button
                    }

                    // Валидация по категориям
                    when (category) {
                        "liquid", "disposable", "snus" -> {
                            if (flavor.isBlank()) {
                                // Можно показать ошибку
                                return@Button
                            }
                        }
                        "vape" -> {
                            if (specification.isBlank()) {
                                // Можно показать ошибку
                                return@Button
                            }
                        }
                    }

                    val newProduct = Product(
                        id = product?.id ?: 0,
                        brand = brand,
                        flavor = when (category) {
                            "liquid", "disposable", "snus" -> flavor
                            else -> brand
                        },
                        barcode = barcode.ifBlank { null },
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        retailPrice = retailPrice.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        category = category,
                        strength = if (category == "liquid") strength else "",
                        specification = if (category == "vape") specification else ""
                    )

                    onSave(newProduct)
                },
                enabled = brand.isNotBlank()
            ) {
                Text(if (product == null) "Добавить" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
