package com.example.vapestoreapp.ui.components

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

@Composable
fun CompactProductTableRow(
    product: Product,
    onEditClick: () -> Unit,
    onUpdateStock: (Int) -> Unit,
    onDeleteClick: () -> Unit
) {
    var stock by remember(product.stock) { mutableIntStateOf(product.stock) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 1.dp), // Минимальные отступы
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Без тени
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            // Первая строка: основная информация
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка категории
                Text(
                    getCategoryEmoji(product.category),
                    modifier = Modifier.padding(end = 4.dp),
                    fontSize = 14.sp // Уменьшили
                )

                // Основная информация - компактно
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, // Уменьшили
                        lineHeight = 14.sp
                    )
                    if (product.flavor.isNotEmpty() && product.flavor != product.brand) {
                        Text(
                            product.flavor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall, // Уменьшили
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 12.sp
                        )
                    }
                }

                // Цены справа
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        "${product.retailPrice} BYN",
                        style = MaterialTheme.typography.bodySmall, // Уменьшили
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 14.sp
                    )
                    Text(
                        "${product.purchasePrice} BYN",
                        style = MaterialTheme.typography.labelSmall, // Уменьшили
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }

            // Вторая строка: управление количеством
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Управление количеством - компактно
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
                        modifier = Modifier.size(24.dp), // Уменьшили
                        enabled = stock > 0
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Уменьшить",
                            modifier = Modifier.size(12.dp), // Уменьшили
                            tint = if (stock > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "$stock шт.",
                        modifier = Modifier.padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.bodySmall, // Уменьшили
                        fontSize = 12.sp
                    )

                    IconButton(
                        onClick = {
                            stock++
                            onUpdateStock(stock)
                        },
                        modifier = Modifier.size(24.dp) // Уменьшили
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Увеличить",
                            modifier = Modifier.size(12.dp), // Уменьшили
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Кнопка установки 0
                    if (stock > 0) {
                        TextButton(
                            onClick = {
                                stock = 0
                                onUpdateStock(0)
                            },
                            modifier = Modifier.padding(start = 2.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("0", fontSize = 10.sp) // Очень маленькая
                        }
                    }
                }

                // Кнопки редактирования и удаления - маленькие
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp) // Уменьшили
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            modifier = Modifier.size(12.dp), // Уменьшили
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp) // Уменьшили
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(12.dp), // Уменьшили
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Третья строка: дополнительная информация (если есть)
            if (product.strength.isNotEmpty() || product.specification.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.strength.isNotEmpty()) {
                        Text(
                            "${product.strength}mg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    if (product.specification.isNotEmpty()) {
                        Text(
                            product.specification,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            title = { Text("Удаление", fontSize = 14.sp) },
            text = {
                Text(
                    "Удалить товар?\n${product.brand}",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
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
