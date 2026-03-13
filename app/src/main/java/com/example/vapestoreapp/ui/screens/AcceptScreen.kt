package com.example.vapestoreapp.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapestoreapp.screens.CameraScanner
import com.example.vapestoreapp.ui.components.PrimaryButton
import com.example.vapestoreapp.ui.components.ScreenScaffold
import com.example.vapestoreapp.ui.components.SectionCard
import com.example.vapestoreapp.ui.components.TonalButton
import com.example.vapestoreapp.ui.components.vapeOutlinedTextFieldColors
import com.example.vapestoreapp.ui.theme.semanticColors
import com.example.vapestoreapp.viewmodel.AcceptViewModel
import com.example.vapestoreapp.viewmodel.AcceptViewModelFactory
import com.example.vapestoreapp.viewmodel.GiftProductData
import com.example.vapestoreapp.viewmodel.NewProductData
import kotlinx.coroutines.delay

private fun getStepNumber(category: String): Int {
    return when (category) {
        "liquid" -> 5
        "vape" -> 4
        "disposable", "snus" -> 4
        else -> 3
    }
}

fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "liquid" -> "Жидкость"
        "disposable" -> "Одноразка"
        "consumable" -> "Расходник"
        "vape" -> "Вейп"
        "snus" -> "Снюс"
        else -> category
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category) {
        "liquid" -> "💧"
        "disposable" -> "🚬"
        "consumable" -> "⚙️"
        "vape" -> "💨"
        "snus" -> ""
        else -> "📦"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun AcceptScreen() {
    val context = LocalContext.current
    val viewModel: AcceptViewModel = viewModel(factory = AcceptViewModelFactory(context))

    // Управление фокусом ввода
    var shouldFocusInput by remember { mutableStateOf(true) }

    // Состояния
    val barcodeInput by viewModel.barcodeInput.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showBrandsDialog by viewModel.showBrandsDialog.collectAsState()
    val showFlavorsDialog by viewModel.showFlavorsDialog.collectAsState()
    val flavors by viewModel.flavors.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val newProductData by viewModel.newProductData.collectAsState()
    val allBrands by viewModel.allBrands.collectAsState()
    val showBrandsDialogForNewProduct by viewModel.showBrandsDialogForNewProduct.collectAsState()
    val allProductsByBrand by viewModel.allProductsByBrand.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val brandDuplicateWarning by viewModel.brandDuplicateWarning.collectAsState()
    val showGiftProductDialog by viewModel.showGiftProductDialog.collectAsState()
    val giftProductData by viewModel.giftProductData.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    // Дополнительные состояния для выбора категории
    var showCategorySelectionDialog by remember { mutableStateOf(false) }
    var selectedCategoryForSelection by remember { mutableStateOf<String?>(null) }
    var showBrandsDialogForSelection by remember { mutableStateOf(false) }
    var showProductsDialogForSelection by remember { mutableStateOf(false) }
    val filteredBrands by viewModel.filteredBrands.collectAsState()
    var selectedBrandForProducts by remember { mutableStateOf<String?>(null) }

    // Функция для проверки формы - валидация данных
    fun isFormValid(data: NewProductData): Boolean {
        if (data.brand.isBlank()) return false

        return when (data.category) {
            "liquid", "disposable", "snus" -> data.flavor.isNotBlank()
            "vape" -> data.specification.isNotBlank()
            else -> true
        }
    }

    // При изменении категории подгружаем соответствующие бренды
    LaunchedEffect(newProductData.category) {
        viewModel.loadBrandsByCategory(newProductData.category)
    }

    var showScannerDialog by remember { mutableStateOf(false) }

    // Фокус для текстового поля
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Автоматически фокусируемся на поле при открытии экрана
    LaunchedEffect(shouldFocusInput) {
        if (shouldFocusInput) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.hide()
        }
    }

    // Автоматически фокусируемся на поле после успешного добавления
    LaunchedEffect(searchResult) {
        if (searchResult.contains("✅") || searchResult.contains("📦")) {
            delay(300)
            focusRequester.requestFocus()
            shouldFocusInput = true
            keyboardController?.hide()
        }
    }

    ScreenScaffold(
        title = "Приемка",
        subtitle = "Сканирование и добавление на склад",
        leadingIcon = Icons.Default.AddCircle
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionCard(
                title = "Штрих‑код",
                subtitle = "Введите вручную или отсканируйте",
                icon = Icons.Default.QrCodeScanner
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = {
                            viewModel.handleBarcodeInput(it)
                            shouldFocusInput = true
                        },
                        label = { Text("Штрих‑код (13 цифр)") },
                        placeholder = { Text("Введите или отсканируйте") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    shouldFocusInput = true
                                }
                            },
                        enabled = true,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.submitBarcode(barcodeInput)
                                keyboardController?.hide()
                            }
                        ),
                        colors = vapeOutlinedTextFieldColors(),
                        interactionSource = remember { MutableInteractionSource() }
                            .also { interactionSource ->
                                val isFocused by interactionSource.collectIsFocusedAsState()
                                LaunchedEffect(isFocused) {
                                    if (isFocused && barcodeInput.isNotEmpty()) {
                                        shouldFocusInput = true
                                    }
                                }
                            }
                    )

                    IconButton(
                        onClick = {
                            viewModel.clearBarcodeInput()
                            shouldFocusInput = true
                            focusRequester.requestFocus()
                        },
                        enabled = barcodeInput.isNotEmpty() && !isLoading
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }

                    IconButton(
                        onClick = {
                            shouldFocusInput = true
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Ввести вручную")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PrimaryButton(
                    onClick = { showScannerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = false },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Сканировать штрих‑код")
                }
            }

        Spacer(modifier = Modifier.height(16.dp))

        // ...
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
        ) {
            if (searchResult.isNotEmpty()) {
                val semantic = MaterialTheme.semanticColors
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (searchResult.contains("✅") || searchResult.contains("📦"))
                            semantic.successContainer
                        else if (searchResult.contains("❗"))
                            semantic.warningContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = searchResult,
                            color = if (searchResult.contains("✅") || searchResult.contains("📦"))
                                semantic.onSuccessContainer
                            else if (searchResult.contains("❗"))
                                semantic.onWarningContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (searchResult.contains("❗")) {
                                // Показываем кнопку добавления только если товар не найден
                                PrimaryButton(
                                    onClick = {
                                        shouldFocusInput = false
                                        viewModel.showAddDialog()
                                    },
                                    modifier = Modifier.focusProperties { canFocus = false }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Добавить новый товар")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            TextButton(
                                onClick = {
                                    viewModel.clearResult()
                                    shouldFocusInput = true
                                    focusRequester.requestFocus()
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.focusProperties { canFocus = false }
                            ) {
                                Text("ОК")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Разделитель
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Выбор из списка
        Text(
            "2. Или выберите из списка:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TonalButton(
            onClick = {
                shouldFocusInput = false  // Убираем фокус с поля ввода
                showCategorySelectionDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false },
            enabled = !isLoading
        ) {
            Icon(Icons.Filled.List, contentDescription = "Список")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать из списка")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка подарочного товара
        TonalButton(
            onClick = {
                shouldFocusInput = false
                viewModel.showGiftProductDialog()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false },
            enabled = !isLoading
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Подарочный товар")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка добавления товара вручную
        TonalButton(
            onClick = {
                shouldFocusInput = false
                viewModel.clearBarcodeInput()
                viewModel.showAddDialog()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить товар вручную")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Инструкция
            SectionCard(
                title = "Подсказка",
                subtitle = "Как работает приемка",
                icon = Icons.Default.Info
            ) {
                Text(
                    "• Введите или отсканируйте штрих‑код товара\n" +
                        "• Количество увеличится в базе\n" +
                        "• Если товара нет — можно добавить вручную",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        // Диалог сканера штрих-кодов
        if (showScannerDialog) {
            Dialog(
                onDismissRequest = {
                    showScannerDialog = false
                    shouldFocusInput = true
                    focusRequester.requestFocus()
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Заголовок
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📷 Сканирование",
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(onClick = {
                                showScannerDialog = false
                                shouldFocusInput = true
                                focusRequester.requestFocus()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }

                        // Камера сканера
                        CameraScanner(
                            onBarcodeScanned = { barcode ->
                                viewModel.handleBarcodeInput(barcode)
                                showScannerDialog = false
                                shouldFocusInput = true
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        )

                        // Подсказка
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Наведите камеру на штрих-код",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "При успешном сканировании окно закроется автоматически",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Кнопка ручного ввода
                        OutlinedButton(
                            onClick = {
                                showScannerDialog = false
                                shouldFocusInput = true
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Ввести вручную")
                        }
                    }
                }
            }
        }
    }

    // Диалог выбора бренда для нового товара (поиск брендов)
    if (showBrandsDialogForNewProduct) {
        AlertDialog(
            onDismissRequest = { viewModel.hideBrandsDialogForNewProduct() },
            title = {
                Text(
                    "🔍 Выберите бренд (${
                        when (newProductData.category) {
                            "liquid" -> "Жидкость"
                            "consumable" -> "Расходники"
                            "vape" -> "Вейпы"
                            "disposable" -> "Одноразки"
                            "snus" -> "Снюс"
                            else -> "Все"
                        }
                    })"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Используем filteredBrands вместо allBrands!
                    // filteredBrands уже содержит бренды только для выбранной категории
                    if (filteredBrands.isEmpty()) {
                        Text(
                            "Нет брендов в этой категории",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(filteredBrands, key = { it }) { brand ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        viewModel.selectBrandForNewProduct(brand)
                                    },
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
                TextButton(
                    onClick = { viewModel.hideBrandsDialogForNewProduct() }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора категории
    if (showCategorySelectionDialog) {
        AlertDialog(
            onDismissRequest = {
                showCategorySelectionDialog = false
                // Очищаем выбранный бренд при закрытии
            },
            title = { Text("Выберите категорию") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    val categoriesList = listOf("liquid", "disposable", "consumable", "vape", "snus")

                    LazyColumn {
                        items(categoriesList, key = { it }) { category ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    // Выбираем категорию
                                    selectedCategoryForSelection = category
                                    showCategorySelectionDialog = false // Закрываем текущий диалог
                                    // Открываем следующий диалог брендов для этой категории
                                    showBrandsDialogForSelection = true
                                    // Загружаем бренды для выбранной категории
                                    viewModel.loadBrandsByCategory(category)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = getCategoryDisplayName(category),
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
                TextButton(
                    onClick = {
                        showCategorySelectionDialog = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора бренда для категории
    if (showBrandsDialogForSelection && selectedCategoryForSelection != null) {
        AlertDialog(
            onDismissRequest = {
                showBrandsDialogForSelection = false
                selectedCategoryForSelection = null
            },
            title = {
                Text(
                    "🏷️ Выберите бренд (${
                        getCategoryDisplayName(selectedCategoryForSelection ?: "")
                    })"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Используем filteredBrands (бренды только этой категории)
                    if (filteredBrands.isEmpty()) {
                        Text(
                            "Нет брендов в этой категории",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(filteredBrands, key = { it }) { brand ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        // Выбираем бренд, загружаем товары
                                        viewModel.loadAllProductsByBrand(brand)
                                        showBrandsDialogForSelection = false
                                        showProductsDialogForSelection = true
                                        selectedBrandForProducts = brand  // Сохраняем выбранный бренд
                                    },
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
                TextButton(
                    onClick = {
                        showBrandsDialogForSelection = false
                        selectedCategoryForSelection = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора товара (показывает товары выбранного бренда)
    if (showProductsDialogForSelection && selectedBrandForProducts != null) {
        AlertDialog(
            onDismissRequest = {
                showProductsDialogForSelection = false
                selectedBrandForProducts = null
            },
            title = {
                Text("📦 Выберите товар")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (allProductsByBrand.isEmpty()) {
                        Text(
                            "Нет товаров для этого бренда",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(allProductsByBrand, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        viewModel.selectFlavor(product)
                                        showProductsDialogForSelection = false
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
                                            // Показываем информацию в зависимости от категории
                                            when (product.category) {
                                                "liquid" -> {
                                                    Text(
                                                        product.flavor,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (product.strength.isNotEmpty()) {
                                                        Text(
                                                            "Крепость: ${product.strength}mg",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                "vape" -> {
                                                    Text(
                                                        product.specification.ifEmpty { product.brand },
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "Вейп",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                "disposable" -> {
                                                    Text(
                                                        product.flavor,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "Одноразка",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                "snus" -> {
                                                    Text(
                                                        product.flavor,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "Снюс",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                else -> {
                                                    Text(
                                                        product.brand,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                            Text(
                                                "${product.retailPrice} BYN",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Badge(
                                            containerColor = if (product.stock > 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ) {
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
                TextButton(
                    onClick = {
                        showProductsDialogForSelection = false
                        selectedBrandForProducts = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора бренда (старая логика)
    if (showBrandsDialog) {
        var showAllProductsDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.hideBrandsDialog() },
            title = { Text("Выберите бренд") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Показываем все бренды без фильтра (как работало изначально)
                    if (allBrands.isEmpty()) {
                        Text(
                            "Нет брендов в базе",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(allBrands, key = { it }) { brand ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        // Загружаем все товары этого бренда (как работало изначально)
                                        selectedBrandForProducts = brand
                                        viewModel.loadAllProductsByBrand(brand)
                                        showAllProductsDialog = true
                                    },
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
                TextButton(
                    onClick = { viewModel.hideBrandsDialog() }
                ) {
                    Text("Отмена")
                }
            }
        )

        // Вложенный диалог для выбора товара (все товары бренда)
        if (showAllProductsDialog && selectedBrandForProducts != null) {
            AlertDialog(
                onDismissRequest = {
                    showAllProductsDialog = false
                    selectedBrandForProducts = null
                },
                title = {
                    Text("📦 Выберите товар")
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        if (allProductsByBrand.isEmpty()) {
                            Text(
                                "Нет товаров для этого бренда",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn {
                                items(allProductsByBrand, key = { it.id }) { product ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        onClick = {
                                            // Принимаем товар (увеличиваем количество)
                                            viewModel.selectFlavor(product)
                                            showAllProductsDialog = false
                                            viewModel.hideBrandsDialog() // Закрываем и родительский диалог
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
                                                // Показываем информацию в зависимости от категории
                                                when (product.category) {
                                                    "liquid" -> {
                                                        Text(
                                                            product.flavor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        if (product.strength.isNotEmpty()) {
                                                            Text(
                                                                "Крепость: ${product.strength}mg",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    "vape" -> {
                                                        Text(
                                                            product.specification.ifEmpty { product.brand },
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            "Вейп",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    "disposable" -> {
                                                        Text(
                                                            product.flavor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            "Одноразка",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    "snus" -> {
                                                        Text(
                                                            product.flavor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            "Снюс",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    else -> {
                                                        Text(
                                                            product.brand,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "${product.retailPrice} BYN",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Badge(
                                                containerColor = if (product.stock > 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            ) {
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
                    TextButton(
                        onClick = {
                            showAllProductsDialog = false
                            selectedBrandForProducts = null
                        }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }

    // Диалог выбора вкуса
    if (showFlavorsDialog && selectedBrand != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideFlavorsDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Выберите вкус")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        selectedBrand!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (flavors.isEmpty()) {
                        Text(
                            "Нет вкусов для этого бренда",
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
                                        viewModel.selectFlavor(product)
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

                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
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
                TextButton(
                    onClick = { viewModel.hideFlavorsDialog() }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подарочного товара
    if (showGiftProductDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredProducts = remember(searchQuery, allProducts) {
            if (searchQuery.isEmpty()) {
                allProducts
            } else {
                allProducts.filter { product ->
                    product.brand.contains(searchQuery, ignoreCase = true) ||
                        product.flavor.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.hideGiftProductDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎁 Подарочный товар")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    // Поиск товаров
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Список товаров
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    viewModel.updateGiftProductData(
                                        GiftProductData(
                                            productId = product.id,
                                            purchasePrice = giftProductData.purchasePrice
                                        )
                                    )
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (giftProductData.productId == product.id)
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            product.brand,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                                            Text(
                                                product.flavor,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "Розница: ${"%.2f".format(product.retailPrice)} BYN",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (giftProductData.productId == product.id) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбранный товар
                    if (giftProductData.productId != null) {
                        val selectedProduct = allProducts.find { it.id == giftProductData.productId }
                        selectedProduct?.let { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Выбран: ${product.brand}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                                        Text(
                                            product.flavor,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Цена закупки
                        OutlinedTextField(
                            value = if (giftProductData.purchasePrice > 0)
                                "%.2f".format(giftProductData.purchasePrice)
                            else "",
                            onValueChange = {
                                val price = it.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (price >= 0 && price <= 9999) {
                                    viewModel.updateGiftProductData(
                                        giftProductData.copy(purchasePrice = price)
                                    )
                                }
                            },
                            label = { Text("Цена закупки (0-9999 BYN)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            prefix = { Text("BYN ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("0.00") },
                            isError = giftProductData.purchasePrice < 0 || giftProductData.purchasePrice > 9999,
                            supportingText = {
                                Text(
                                    "Введите цену закупки от 0 до 9999 BYN",
                                    color = if (giftProductData.purchasePrice < 0 || giftProductData.purchasePrice > 9999)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addGiftProduct() },
                    enabled = giftProductData.productId != null &&
                        giftProductData.purchasePrice >= 0 &&
                        giftProductData.purchasePrice <= 9999
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideGiftProductDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог добавления нового товара
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddDialog() },
            title = { Text("➕ Добавление нового товара") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Категория
                    Text("1. Категория:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.categories.forEach { (id, name) ->
                            FilterChip(
                                selected = newProductData.category == id,
                                onClick = {
                                    viewModel.updateNewProductData(
                                        newProductData.copy(category = id)
                                    )
                                    viewModel.loadBrandsByCategory(id)
                                },
                                label = { Text(name) },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Бренд
                    Text("2. Выберите бренд:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newProductData.brand,
                            onValueChange = {
                                viewModel.updateNewProductData(
                                    newProductData.copy(brand = it)
                                )
                            },
                            label = { Text("Название бренда *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    when (newProductData.category) {
                                        "liquid" -> "Например: PODONKI CRITICAL"
                                        "consumable" -> "Например: Испаритель aegis boost 0.2"
                                        "vape" -> "Например: XROS 5 MINI"
                                        else -> "Название бренда"
                                    }
                                )
                            }
                        )

                        Button(
                            onClick = { viewModel.showBrandsDialogForNewProduct() },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Выбрать из списка")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Крепость (только для жидкостей)
                    if (newProductData.category == "liquid") {
                        Text("3. Крепость (mg):", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = newProductData.strength,
                            onValueChange = {
                                val digitsOnly = it.filter { char -> char.isDigit() }
                                viewModel.updateNewProductData(
                                    newProductData.copy(strength = digitsOnly)
                                )
                            },
                            label = { Text("Крепость в mg") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Например: 50") },
                            suffix = { Text("mg") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4. Вкус (только для жидкостей, одноразок и снюса)
                    if (newProductData.category == "liquid" ||
                        newProductData.category == "disposable" ||
                        newProductData.category == "snus"
                    ) {

                        Text(
                            "${if (newProductData.category == "liquid") "4" else "3"}. Вкус:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = newProductData.flavor,
                            onValueChange = {
                                viewModel.updateNewProductData(
                                    newProductData.copy(flavor = it)
                                )
                            },
                            label = {
                                Text(
                                    when (newProductData.category) {
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
                                    when (newProductData.category) {
                                        "liquid" -> "Например: Клубничный джем"
                                        "disposable" -> "Например: Яблоко персик"
                                        "snus" -> "Например: Вишня"
                                        else -> "Вкус"
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 5. Цвет (только для вейпов)
                    if (newProductData.category == "vape") {
                        Text(
                            "${if (newProductData.category == "liquid") "5" else "4"}. Цвет:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = newProductData.specification,
                            onValueChange = {
                                viewModel.updateNewProductData(
                                    newProductData.copy(specification = it)
                                )
                            },
                            label = { Text("Цвет *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Например: PURPLE, PASTEL CRYSTAL") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 5. Штрих-код
                    Text(
                        "${getStepNumber(newProductData.category)}. Штрих-код:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = newProductData.barcode,
                        onValueChange = {
                            viewModel.updateNewProductData(
                                newProductData.copy(barcode = it)
                            )
                        },
                        label = { Text("13 цифр") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        readOnly = newProductData.barcode.length == 13
                    )

                    // 6. Количество
                    if (newProductData.category == "liquid") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("7. Начальное количество:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Добавьте начальное количество в базу (или оставьте 1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (newProductData.quantity > 1) {
                                        viewModel.updateNewProductData(
                                            newProductData.copy(quantity = newProductData.quantity - 1)
                                        )
                                    }
                                },
                                enabled = newProductData.quantity > 1,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                            }
                            Text(
                                "${newProductData.quantity}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (newProductData.quantity < 10) {
                                        viewModel.updateNewProductData(
                                            newProductData.copy(quantity = newProductData.quantity + 1)
                                        )
                                    }
                                },
                                enabled = newProductData.quantity < 10,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Увеличить")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "штук",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 8. Цены
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${if (newProductData.category == "liquid") "8" else "6"}. Цены:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newProductData.purchasePrice,
                            onValueChange = {
                                viewModel.updateNewProductData(
                                    newProductData.copy(purchasePrice = it)
                                )
                            },
                            label = { Text("Закупка") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            prefix = { Text("BYN ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0.00") }
                        )

                        OutlinedTextField(
                            value = newProductData.retailPrice,
                            onValueChange = {
                                viewModel.updateNewProductData(
                                    newProductData.copy(retailPrice = it)
                                )
                            },
                            label = { Text("Розница") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            prefix = { Text("BYN ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0.00") }
                        )
                    }

                    // Предпросмотр
                    Spacer(modifier = Modifier.height(16.dp))
                    if (newProductData.brand.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Товар будет добавлен как:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    buildString {
                                        when (newProductData.category) {
                                            "liquid" -> {
                                                val strength = if (newProductData.strength.isNotEmpty())
                                                    " ${newProductData.strength}mg" else ""
                                                append("${newProductData.brand}$strength")
                                                if (newProductData.flavor.isNotEmpty()) {
                                                    append(" - ${newProductData.flavor}")
                                                }
                                            }

                                            else -> {
                                                append(newProductData.brand)
                                            }
                                        }
                                        if (newProductData.retailPrice.isNotEmpty()) {
                                            append(" (${newProductData.retailPrice} BYN)")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addNewProduct() },
                    enabled = isFormValid(newProductData)
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideAddDialog() }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
}