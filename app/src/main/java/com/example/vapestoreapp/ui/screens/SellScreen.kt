package com.example.vapestoreapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.ui.components.IconLabelFilterChip
import com.example.vapestoreapp.ui.components.ScreenScaffold
import com.example.vapestoreapp.ui.components.SectionCard
import com.example.vapestoreapp.ui.components.vapeOutlinedTextFieldColors
import com.example.vapestoreapp.utils.displaySubtitle
import com.example.vapestoreapp.viewmodel.SellViewModel
import com.example.vapestoreapp.viewmodel.SellViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen() {
    val context = LocalContext.current
    val viewModel: SellViewModel = viewModel(factory = SellViewModelFactory(context))

    // Состояния
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val comment by viewModel.comment.collectAsState()
    val saleResult by viewModel.saleResult.collectAsState()
    val showFlavorsDialog by viewModel.showFlavorsDialog.collectAsState()
    val flavors by viewModel.flavors.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val paymentSelection by viewModel.paymentSelection.collectAsState()
    val splitCashInput by viewModel.splitCashInput.collectAsState()
    val splitCardInput by viewModel.splitCardInput.collectAsState()
    val splitPaymentUi by viewModel.splitPaymentUi.collectAsState()
    val saleType by viewModel.saleType.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val expirationDate by viewModel.expirationDate.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()
    val showBrandsDialogForCategory by viewModel.showBrandsDialogForCategory.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val brandsForCategory by viewModel.brandsForCategory.collectAsState()
    val paymentCards by viewModel.paymentCards.collectAsState()
    val cardQuery by viewModel.cardQuery.collectAsState()
    val selectedCardId by viewModel.selectedCardId.collectAsState()

    // Локальное состояние для поиска
    var localSearchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Продажа",
        subtitle = "Быстрая продажа, долг или бронь",
        leadingIcon = Icons.Default.ShoppingCart
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionCard(
                title = "Товар",
                subtitle = "Поиск по штрих‑коду или названию",
                icon = Icons.Default.Search
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localSearchQuery,
                        onValueChange = {
                            localSearchQuery = it
                            showSearchResults = it.isNotEmpty()
                        },
                        label = { Text("Штрих‑код или название") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Начните вводить…") },
                        colors = vapeOutlinedTextFieldColors(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = if (localSearchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { localSearchQuery = ""; showSearchResults = false }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Очистить",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null
                    )

                    FilledTonalButton(
                        onClick = { viewModel.showCategoryDialog() },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Список")
                    }
                }
            }

        if (showSearchResults && localSearchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val searchResults = viewModel.searchProducts(localSearchQuery)

            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        searchResults.forEach { product ->
                            ProductItem(
                                product = product,
                                onClick = {
                                    viewModel.selectProduct(product)
                                    showSearchResults = false
                                    localSearchQuery = product.brand
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Товары не найдены",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedProduct != null) {
            Text(
                "2. Выбранный товар:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            selectedProduct?.let { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    product.brand,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    product.flavor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                Text("${product.stock} шт.")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Закупка: ${product.purchasePrice} BYN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                "Розница: ${product.retailPrice} BYN",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.resetSaleCompletely()
                                localSearchQuery = ""
                                showSearchResults = true
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Сбросить выбор")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "3. Количество:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) viewModel.updateQuantity(quantity - 1) },
                        enabled = quantity > 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                    }

                    Text(
                        text = "$quantity шт.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = {
                            if (selectedProduct?.stock ?: 0 > quantity) {
                                viewModel.updateQuantity(quantity + 1)
                            }
                        },
                        enabled = (selectedProduct?.stock ?: 0) > quantity,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить")
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        listOf(1, 2, 3, 5).forEach { qty ->
                            FilterChip(
                                selected = quantity == qty,
                                onClick = { viewModel.updateQuantity(qty) },
                                label = { Text("$qty") },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "4. Метод оплаты:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconLabelFilterChip(
                        selected = paymentSelection.contains("cash"),
                        onClick = { viewModel.togglePaymentMethod("cash") },
                        icon = Icons.Default.Payments,
                        label = "Наличные",
                        modifier = Modifier.weight(1f)
                    )

                    IconLabelFilterChip(
                        selected = paymentSelection.contains("card"),
                        onClick = { viewModel.togglePaymentMethod("card") },
                        icon = Icons.Default.CreditCard,
                        label = "Карта",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (saleType == "sale" && paymentSelection.contains("card")) {
                    Spacer(modifier = Modifier.height(10.dp))

                    var isCardMenuExpanded by remember { mutableStateOf(false) }
                    val filteredCards = remember(cardQuery, paymentCards) {
                        val q = cardQuery.trim()
                        if (q.isEmpty()) paymentCards
                        else paymentCards.filter { it.label.contains(q, ignoreCase = true) }
                    }

                    ExposedDropdownMenuBox(
                        expanded = isCardMenuExpanded,
                        onExpandedChange = { isCardMenuExpanded = !isCardMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = cardQuery,
                            onValueChange = {
                                viewModel.updateCardQuery(it)
                                isCardMenuExpanded = true
                            },
                            label = { Text("Карта (последние 4 цифры)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            placeholder = { Text("Например: 1234") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCardMenuExpanded) },
                            supportingText = {
                                val selectedLabel =
                                    selectedCardId?.let { id -> paymentCards.find { it.id == id }?.label }
                                if (selectedLabel != null && selectedLabel != cardQuery) {
                                    Text("Выбрано: $selectedLabel")
                                }
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = isCardMenuExpanded,
                            onDismissRequest = { isCardMenuExpanded = false }
                        ) {
                            filteredCards.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text(card.label) },
                                    onClick = {
                                        viewModel.selectCard(card)
                                        isCardMenuExpanded = false
                                    }
                                )
                            }

                            val canAdd = cardQuery.trim().isNotEmpty() &&
                                paymentCards.none { it.label.equals(cardQuery.trim(), ignoreCase = true) }

                            if (canAdd) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("➕ Добавить \"${cardQuery.trim()}\"") },
                                    onClick = {
                                        viewModel.addCard(cardQuery)
                                        isCardMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (saleType == "sale" && paymentMethod == "split") {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Раздельная оплата:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = splitCashInput,
                            onValueChange = { viewModel.updateSplitCashInput(it) },
                            label = { Text("Наличные") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("BYN ") },
                            placeholder = { Text("0.00") },
                            isError = splitCashInput.isNotEmpty() && !splitPaymentUi.isValid
                        )

                        OutlinedTextField(
                            value = splitCardInput,
                            onValueChange = { viewModel.updateSplitCardInput(it) },
                            label = { Text("Карта") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("BYN ") },
                            placeholder = { Text("0.00") },
                            isError = splitCardInput.isNotEmpty() && !splitPaymentUi.isValid
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Итого к оплате: ${"%.2f".format(splitPaymentUi.totalDue)} BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!splitPaymentUi.isValid && (splitCashInput.isNotEmpty() || splitCardInput.isNotEmpty())) {
                        Text(
                            splitPaymentUi.error ?: "Неверная сумма",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "5. Тип продажи:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = saleType == "sale",
                        onClick = { viewModel.updateSaleType("sale") },
                        label = { Text("Продажа") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = saleType == "debt",
                        onClick = { viewModel.updateSaleType("debt") },
                        label = { Text("В долг") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = saleType == "reservation",
                        onClick = { viewModel.updateSaleType("reservation") },
                        label = { Text("Бронь") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (saleType == "debt" || saleType == "reservation") {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.updateCustomerName(it) },
                        label = { Text("Имя клиента") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Введите имя клиента") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    if (saleType == "reservation") {
                        Spacer(modifier = Modifier.height(8.dp))

                        var showDatePicker by remember { mutableStateOf(false) }
                        var showTimePicker by remember { mutableStateOf(false) }

                        Text(
                            "Дата и время окончания брони:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                val exp = expirationDate
                                Text(
                                    exp?.let { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it)) }
                                        ?: "Выбрать дату"
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (expirationDate == null) {
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 7)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        viewModel.updateExpirationDate(cal.timeInMillis)
                                    }
                                    showTimePicker = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                val exp = expirationDate
                                Text(
                                    exp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
                                        ?: "Выбрать время"
                                )
                            }
                        }

                        if (showDatePicker) {
                            val exp = expirationDate
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = exp ?: System.currentTimeMillis()
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
                                                    datePickerState.selectedDateMillis?.let {
                                                        val selectedCalendar = Calendar.getInstance()
                                                        selectedCalendar.timeInMillis = it
                                                        val currentCalendar = if (exp != null) {
                                                            Calendar.getInstance().apply { timeInMillis = exp }
                                                        } else {
                                                            Calendar.getInstance()
                                                        }
                                                        selectedCalendar.set(
                                                            Calendar.HOUR_OF_DAY,
                                                            currentCalendar.get(Calendar.HOUR_OF_DAY)
                                                        )
                                                        selectedCalendar.set(
                                                            Calendar.MINUTE,
                                                            currentCalendar.get(Calendar.MINUTE)
                                                        )
                                                        viewModel.updateExpirationDate(selectedCalendar.timeInMillis)
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
                            val exp = expirationDate
                            val cal = Calendar.getInstance().apply {
                                if (exp != null) timeInMillis = exp
                            }
                            val timePickerState = rememberTimePickerState(
                                initialHour = if (exp != null) cal.get(Calendar.HOUR_OF_DAY)
                                else Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                initialMinute = if (exp != null) cal.get(Calendar.MINUTE)
                                else Calendar.getInstance().get(Calendar.MINUTE)
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
                                                    val selectedCalendar = Calendar.getInstance()
                                                    if (exp != null) selectedCalendar.timeInMillis = exp
                                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                    selectedCalendar.set(Calendar.MINUTE, timePickerState.minute)
                                                    selectedCalendar.set(Calendar.SECOND, 0)
                                                    selectedCalendar.set(Calendar.MILLISECOND, 0)
                                                    viewModel.updateExpirationDate(selectedCalendar.timeInMillis)
                                                    showTimePicker = false
                                                }
                                            ) { Text("OK") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "6. Скидка (BYN):",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                var discountText by remember { mutableStateOf("") }
                val maxDiscount = remember(selectedProduct, quantity) { viewModel.getMaxDiscount() }
                val isDiscountValid = remember(discountText, maxDiscount) {
                    val value = discountText.toDoubleOrNull() ?: 0.0
                    value in 0.0..maxDiscount
                }

                OutlinedTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        val newValue = it.toDoubleOrNull() ?: 0.0
                        viewModel.updateDiscount(newValue)
                    },
                    label = { Text("Сумма скидки") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("BYN ") },
                    placeholder = { Text("0.00") },
                    isError = discountText.isNotEmpty() && !isDiscountValid,
                    supportingText = {
                        if (discountText.isNotEmpty() && !isDiscountValid) {
                            Text(
                                "Максимальная скидка: ${"%.2f".format(maxDiscount)} BYN",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (maxDiscount > 0) {
                            Text(
                                "Макс: ${"%.2f".format(maxDiscount)} BYN",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "7. Комментарий (необязательно):",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { viewModel.updateComment(it) },
                    label = { Text("Комментарий к продаже") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Например: VIP клиент") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.sellProduct() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = (selectedProduct?.stock ?: 0) > 0 &&
                        (saleType == "sale" || customerName.isNotBlank()) &&
                        (saleType != "sale" || paymentMethod != "split" || splitPaymentUi.isValid),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp,
                        disabledElevation = 0.dp
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        when (saleType) {
                            "debt" -> Icons.Default.AccountBalance
                            "reservation" -> Icons.Default.Bookmark
                            else -> Icons.Default.ShoppingCart
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        when (saleType) {
                            "debt" -> "ОФОРМИТЬ ДОЛГ"
                            "reservation" -> "ОФОРМИТЬ БРОНЬ"
                            else -> "ПРОДАТЬ ТОВАР"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    "Товар не выбран",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (saleResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -40 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saleResult.contains("✅") || saleResult.contains("📦"))
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 16.dp
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (saleResult.contains("✅") || saleResult.contains("📦"))
                                Icons.Filled.CheckCircle
                            else
                                Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (saleResult.contains("✅") || saleResult.contains("📦"))
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = saleResult,
                            color = if (saleResult.contains("✅") || saleResult.contains("📦"))
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    viewModel.resetSaleCompletely()
                                    localSearchQuery = ""
                                    showSearchResults = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Новая продажа")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetSaleCompletely()
                                    localSearchQuery = ""
                                    showSearchResults = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Закрыть")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (saleResult.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "ℹ️ Инструкция:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Введите название или штрих-код\n" +
                            "• Или выберите из списка категорий\n" +
                            "• Для жидкостей: бренд -> вкус\n" +
                            "• Для расходников/вейпов: только бренд",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCategoryDialog) {
                        AlertDialog(
            onDismissRequest = { viewModel.hideCategoryDialog() },
                            title = { Text("Выберите категорию") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    val categories = listOf(
                        "liquid",
                        "disposable",
                        "consumable",
                        "vape",
                        "snus"
                    )

                    LazyColumn {
                        items(categories, key = { it }) { id ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { viewModel.selectCategory(id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = getCategoryDisplayName(id),
                                    modifier = Modifier.padding(16.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideCategoryDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showBrandsDialogForCategory && selectedCategory != null) {
                        AlertDialog(
            onDismissRequest = { viewModel.hideBrandsDialogForCategory() },
            title = {
                Text(
                                    "Выберите бренд (${
                        getCategoryDisplayName(selectedCategory ?: "")
                    })"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (brandsForCategory.isEmpty()) {
                        Text(
                            "Нет брендов в данной категории",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(brandsForCategory, key = { it }) { brand ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { viewModel.loadProductsByBrandForCategory(brand) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = brand,
                                        modifier = Modifier.padding(16.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideBrandsDialogForCategory() }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showFlavorsDialog) {
                        AlertDialog(
            onDismissRequest = { viewModel.hideFlavorsDialog() },
                            title = { Text("Выберите вкус") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (flavors.isEmpty()) {
                        Text(
                            "Нет товаров в наличии",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(flavors, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        viewModel.selectProduct(product)
                                        viewModel.hideFlavorsDialog()
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                product.flavor,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${product.retailPrice} BYN",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("${product.stock} шт.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideFlavorsDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductItem(product: Product, onClick: () -> Unit) {
    val subtitle = product.displaySubtitle()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 4.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.brand,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${product.retailPrice} BYN",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = if (product.stock > 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (product.stock > 0)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(
                        "${product.stock} шт.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}