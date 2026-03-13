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
fun ReservationsScreen() {
    val context = LocalContext.current
    val viewModel: ReservationsViewModel = viewModel(factory = ReservationsViewModelFactory(context))
    val reservations by viewModel.reservations.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    if (showCreateDialog) {
        var customerName by remember { mutableStateOf("") }
        var productQuery by remember { mutableStateOf("") }
        var isProductMenuExpanded by remember { mutableStateOf(false) }
        var selectedProductId by remember { mutableStateOf<Int?>(null) }
        var quantityText by remember { mutableStateOf("1") }

        val defaultExpirationMillis = remember {
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        }
        var expirationMillis by remember { mutableStateOf<Long?>(defaultExpirationMillis) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val filteredProducts = remember(productQuery, allProducts) {
            val q = productQuery.trim()
            val base = allProducts.asSequence()
                .filter { it.stock > 0 }

            if (q.isEmpty()) {
                base.take(30).toList()
            } else {
                base.filter { p ->
                    p.displayName().contains(q, ignoreCase = true) ||
                        p.brand.contains(q, ignoreCase = true) ||
                        p.flavor.contains(q, ignoreCase = true) ||
                        p.specification.contains(q, ignoreCase = true)
                }.take(30).toList()
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.hideCreateReservationDialog() },
            title = { Text("Создать резерв") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Имя клиента") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    ExposedDropdownMenuBox(
                        expanded = isProductMenuExpanded,
                        onExpandedChange = { isProductMenuExpanded = !isProductMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = productQuery,
                            onValueChange = {
                                productQuery = it
                                isProductMenuExpanded = true
                                selectedProductId = null
                            },
                            label = { Text("Товар (поиск)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductMenuExpanded) },
                            singleLine = true,
                            placeholder = { Text("Начните вводить…") }
                        )

                        ExposedDropdownMenu(
                            expanded = isProductMenuExpanded,
                            onDismissRequest = { isProductMenuExpanded = false }
                        ) {
                            filteredProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.displayName(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectedProductId = p.id
                                        productQuery = p.displayName()
                                        isProductMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Количество") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) }
                    )

                    Text(
                        "Дата и время окончания резерва:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                expirationMillis?.let { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it)) }
                                    ?: "Выбрать дату"
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (expirationMillis == null) expirationMillis = System.currentTimeMillis()
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                expirationMillis?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
                                    ?: "Выбрать время"
                            )
                        }
                    }

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = expirationMillis ?: System.currentTimeMillis()
                        )
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showDatePicker = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    DatePicker(state = datePickerState)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
                                        TextButton(
                                            onClick = {
                                                datePickerState.selectedDateMillis?.let { selectedDate ->
                                                    val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                                    val currentCalendar = Calendar.getInstance().apply {
                                                        timeInMillis = expirationMillis ?: System.currentTimeMillis()
                                                    }
                                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                                                    selectedCalendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                                                    selectedCalendar.set(Calendar.SECOND, 0)
                                                    selectedCalendar.set(Calendar.MILLISECOND, 0)
                                                    expirationMillis = selectedCalendar.timeInMillis
                                                }
                                                showDatePicker = false
                                            }
                                        ) { Text("OK") }
                                    }
                                }
                            }
                        }
                    }

                    if (showTimePicker) {
                        val currentCalendar = remember(expirationMillis) {
                            Calendar.getInstance().apply { timeInMillis = expirationMillis ?: System.currentTimeMillis() }
                        }
                        val timePickerState = rememberTimePickerState(
                            initialHour = currentCalendar.get(Calendar.HOUR_OF_DAY),
                            initialMinute = currentCalendar.get(Calendar.MINUTE)
                        )
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    TimePicker(state = timePickerState)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                                        TextButton(
                                            onClick = {
                                                val cal = Calendar.getInstance().apply {
                                                    timeInMillis = expirationMillis ?: System.currentTimeMillis()
                                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                    set(Calendar.MINUTE, timePickerState.minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                expirationMillis = cal.timeInMillis
                                                showTimePicker = false
                                            }
                                        ) { Text("OK") }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val productId = selectedProductId
                        val qty = quantityText.toIntOrNull() ?: 0
                        val exp = expirationMillis
                        if (productId != null && exp != null) {
                            viewModel.createReservation(
                                customerName = customerName,
                                productId = productId,
                                quantity = qty,
                                expirationDateTimeMillis = exp
                            )
                        } else {
                            // Дадим ViewModel показать ошибку через operationResult
                            viewModel.createReservation(
                                customerName = customerName,
                                productId = productId ?: 0,
                                quantity = qty,
                                expirationDateTimeMillis = exp ?: 0L
                            )
                        }
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCreateReservationDialog() }) { Text("Отмена") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Заголовок с иконкой
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Резервы",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { viewModel.showCreateReservationDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (operationResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (operationResult.startsWith("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        operationResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (operationResult.startsWith("✅"))
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = { viewModel.clearResult() }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            }
        }

        if (reservations.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Нет активных резервов",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "💡 Как использовать:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow(
                            icon = Icons.Default.ShoppingCart,
                            text = "При продаже выберите тип \"Резерв\" и укажите имя клиента"
                        )
                        InfoRow(
                            icon = Icons.Default.Schedule,
                            text = "Установите срок резерва (по умолчанию 7 дней)"
                        )
                        InfoRow(
                            icon = Icons.Default.Bookmark,
                            text = "Товар будет зарезервирован и не будет доступен для других продаж"
                        )
                        InfoRow(
                            icon = Icons.Default.CheckCircle,
                            text = "Когда клиент заберёт товар, нажмите \"Продать\" в карточке резерва"
                        )
                        InfoRow(
                            icon = Icons.Default.Info,
                            text = "После продажи резерва создастся продажа и товар спишется со склада"
                        )
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reservations, key = { it.id }) { res ->
                    val isExpired = viewModel.isExpired(res)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpired)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        res.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${viewModel.getProductName(res.productId)} × ${res.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isExpired) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("Просрочено", fontSize = 10.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "До: ${viewModel.formatDate(res.expirationDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.sellReservation(res.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 6.dp
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Продать", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelReservation(res.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Отменить", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
