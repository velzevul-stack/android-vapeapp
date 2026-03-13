package com.example.vapestoreapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.viewmodel.CabinetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
fun DragDropProductList(
    viewModel: CabinetViewModel,
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    onUpdateStock: (Int, Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var reorderEnabled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Группируем товары по категориям, затем по брендам
    val (productsByCategory, brandsByCategory) = remember(products, reorderEnabled) {
        if (reorderEnabled) {
            // Группируем по категориям
            val categoryGroups = products.groupBy { it.category }
                .toSortedMap(compareBy {
                    when (it) {
                        "liquid" -> 1
                        "disposable" -> 2
                        "consumable" -> 3
                        "vape" -> 4
                        "snus" -> 5
                        else -> 6
                    }
                })

            // Для каждой категории группируем по брендам
            val brandGroups = categoryGroups.mapValues { (_, prods) ->
                prods.groupBy { it.brand }
            }

            Pair(categoryGroups, brandGroups)
        } else {
            Pair(emptyMap<String, List<Product>>(), emptyMap<String, Map<String, List<Product>>>())
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Панель управления
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (reorderEnabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (reorderEnabled) "📋 Режим упорядочивания" else "📋 Список товаров",
                    style = MaterialTheme.typography.titleSmall
                )

                Row {
                    if (!reorderEnabled) {
                        Button(
                            onClick = {
                                reorderEnabled = true
                            },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Упорядочить")
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.refreshFilteredProducts()
                                    reorderEnabled = false
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Сохранить")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                reorderEnabled = false
                            },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }

        if (reorderEnabled) {
            Text(
                "• Используйте стрелки для изменения порядка брендов\n" +
                        "• Нажмите ✏️ чтобы переименовать бренд\n" +
                        "• Все вкусы этого бренда будут изменены",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            )
        }

        if (products.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Нет товаров",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (reorderEnabled) {
                // В режиме упорядочивания - показываем только бренды со стрелками
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    productsByCategory.forEach { (category, categoryProducts) ->
                        item {
                            CategoryHeader(category = category, count = categoryProducts.size)
                        }

                        // Получаем бренды для этой категории
                        val brandsInCategory = brandsByCategory[category] ?: emptyMap()
                        val brandList = brandsInCategory.keys.toList()

                        items(brandList, key = { it }) { brand ->
                            val brandProducts = brandsInCategory[brand] ?: emptyList()

                            // Карточка бренда со стрелками для перемещения
                            ReorderBrandCard(
                                brand = brand,
                                productsCount = brandProducts.size,
                                currentIndex = brandList.indexOf(brand),
                                totalItems = brandList.size,
                                category = category,
                                viewModel = viewModel,
                                coroutineScope = coroutineScope,
                                onReorder = {
                                    coroutineScope.launch {
                                        viewModel.refreshFilteredProducts()
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            } else {
                // В обычном режиме - обычный список товаров
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        SimpleProductItem(
                            product = product,
                            onClick = { onProductClick(product) },
                            onUpdateStock = { newStock ->
                                onUpdateStock(product.id, newStock)
                            },
                            onDeleteClick = { onDeleteClick(product.id) },
                            onEditBrand = {
                                // Открываем диалог редактирования бренда
                                viewModel.openEditBrandDialog(product.brand)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderBrandCard(
    brand: String,
    productsCount: Int,
    currentIndex: Int,
    totalItems: Int,
    category: String,
    viewModel: CabinetViewModel,
    coroutineScope: CoroutineScope,
    onReorder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMoving by remember { mutableStateOf(false) }
    var localIndex by remember { mutableIntStateOf(currentIndex) }

    // Синхронизируем локальный индекс с реальным
    LaunchedEffect(currentIndex) {
        localIndex = currentIndex
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isMoving) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Стрелки для перемещения
            Column {
                // Стрелка вверх
                IconButton(
                    onClick = {
                        if (localIndex > 0) {
                            // Мгновенно обновляем локальное состояние
                            localIndex--
                            isMoving = true

                            // Запускаем перемещение
                            viewModel.moveBrandUpOptimized(category, brand)
                            onReorder()

                            // Сбрасываем состояние через 300мс
                            coroutineScope.launch {
                                delay(300)
                                isMoving = false
                            }
                        }
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = localIndex > 0
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Поднять выше",
                        modifier = Modifier.size(16.dp),
                        tint = if (localIndex > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Стрелка вниз
                IconButton(
                    onClick = {
                        if (localIndex < totalItems - 1) {
                            // Мгновенно обновляем локальное состояние
                            localIndex++
                            isMoving = true

                            // Запускаем перемещение
                            viewModel.moveBrandDownOptimized(category, brand)
                            onReorder()

                            // Сбрасываем состояние через 300мс
                            coroutineScope.launch {
                                delay(300)
                                isMoving = false
                            }
                        }
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = localIndex < totalItems - 1
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Опустить ниже",
                        modifier = Modifier.size(16.dp),
                        tint = if (localIndex < totalItems - 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Информация о бренде
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        brand,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Кнопка редактирования бренда
                    IconButton(
                        onClick = {
                            viewModel.openEditBrandDialog(brand)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать бренд",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "$productsCount товар${getPluralForm(productsCount)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Номер позиции
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    "${localIndex + 1}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SimpleProductItem(
    product: Product,
    onClick: () -> Unit,
    onUpdateStock: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onEditBrand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stock by remember(product.stock) { mutableIntStateOf(product.stock) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 2.dp) // Увеличил отступ
            .clip(RoundedCornerShape(6.dp)), // Более круглые углы
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Немного тени
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp) // Увеличил отступы
        ) {
            // Первая строка: бренд и иконка категории
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка категории
                Text(
                    getCategoryEmoji(product.category),
                    modifier = Modifier.padding(end = 6.dp),
                    fontSize = 16.sp // Увеличил иконку
                )

                // Бренд
                Text(
                    product.brand,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Цена розничная
                Text(
                    "${product.retailPrice} BYN",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 16.sp
                )
            }

            // Вторая строка: вкус (если есть)
            if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    product.flavor,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    maxLines = 2, // Разрешаем 2 строки для вкуса
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Третья строка: доп. информация и управление
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая часть: доп. информация
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Цена закупочная
                    Text(
                        "Закуп: ${product.purchasePrice} BYN",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Крепость для жидкостей
                    if (product.strength.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "• ${product.strength}mg",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Спецификация (цвет для вейпов)
                    if (product.specification.isNotEmpty() && product.category != "liquid") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "• ${product.specification}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Правая часть: управление
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Управление количеством
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (stock > 0) {
                                    stock--
                                    onUpdateStock(stock)
                                }
                            },
                            modifier = Modifier.size(28.dp), // Увеличил кнопки
                            enabled = stock > 0
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Уменьшить",
                                modifier = Modifier.size(14.dp), // Увеличил иконки
                                tint = if (stock > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$stock шт.",
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(40.dp) // Увеличил ширину
                                .wrapContentWidth(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp) // Увеличил текст
                        )

                        IconButton(
                            onClick = {
                                stock++
                                onUpdateStock(stock)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Увеличить",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка редактирования товара
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать товар",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Кнопка редактирования бренда
                    IconButton(
                        onClick = onEditBrand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Brush,
                            contentDescription = "Редактировать бренд",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Кнопка удаления
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("Удаление", fontSize = 14.sp)
            },
            text = {
                Column {
                    Text(
                        "Удалить товар?",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        product.brand,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                        Text(
                            product.flavor,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Удалить",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Отмена", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
fun CategoryHeader(category: String, count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    getCategoryEmoji(category),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    getCategoryName(category),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Badge(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("$count шт.")
            }
        }
    }
}

private fun getPluralForm(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> ""
        count % 10 in 2..4 && count % 100 !in 12..14 -> "а"
        else -> "ов"
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "liquid" -> "🍬"
        "disposable" -> "🚬"
        "consumable" -> "⚙️"
        "vape" -> "🔋"
        "snus" -> "🫀"
        else -> "📦"
    }
}

private fun getCategoryName(category: String): String {
    return when (category) {
        "liquid" -> "Жидкости"
        "disposable" -> "Одноразки"
        "consumable" -> "Расходники"
        "vape" -> "Вейпы"
        "snus" -> "Снюс"
        else -> "Другие"
    }
}