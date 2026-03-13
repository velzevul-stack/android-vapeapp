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

@Composable
fun SalesScreen(
    viewModel: CabinetViewModel
) {
    val salesByDay by viewModel.salesByDay.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val totalProfit by viewModel.totalProfit.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val showDayDetailsDialog by viewModel.showDayDetailsDialog.collectAsState()
    val selectedDayForDetails by viewModel.selectedDayForDetails.collectAsState()

    // Состояния для диалога произвольного периода
    var showCustomPeriodDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    // ИСПРАВЛЕНИЕ: Используем Box с ограничением по высоте
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💰 Продажи",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Выбор периода
            Text(
                "Период:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопки сезонов - делаем компактнее
            val seasons = Period.getSeasons()

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = selectedPeriod?.name == season.name,
                        onClick = {
                            if (selectedPeriod?.name == season.name) {
                                viewModel.loadSalesData(null)
                            } else {
                                viewModel.loadSalesData(season)
                            }
                        },
                        label = {
                            Text(
                                season.name,
                                fontSize = 14.sp
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.height(40.dp)
                    )
                }

                // Кнопка "Все время"
                FilterChip(
                    selected = selectedPeriod == null,
                    onClick = { viewModel.loadSalesData(null) },
                    label = {
                        Text("Все", fontSize = 14.sp)
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка произвольного периода
            OutlinedButton(
                onClick = { showCustomPeriodDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Выбрать период",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Итоговая выручка и прибыль
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedPeriod != null) {
                        Text(
                            selectedPeriod?.name ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Выручка:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${"%.2f".format(totalRevenue)} BYN",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Прибыль:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${"%.2f".format(totalProfit)} BYN",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Список дней с продажами
            Text(
                "Продажи:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (salesByDay.isEmpty()) {
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
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp), // Уменьшили иконку
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Нет продаж",
                            style = MaterialTheme.typography.bodySmall, // Уменьшили
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Список продаж
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(salesByDay, key = { it.dayTimestamp }) { day ->
                        CompactSalesDayCard(
                            day = day,
                            onClick = { viewModel.openDayDetails(day) }
                        )
                    }

                    // Добавляем отступ внизу
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Диалог детализации дня
    if (showDayDetailsDialog && selectedDayForDetails != null) {
        DaySalesDetailsDialog(
            day = selectedDayForDetails!!,
            onDismiss = { viewModel.closeDayDetails() }
        )
    }

    // Диалог произвольного периода
    if (showCustomPeriodDialog) {
        CustomPeriodDialog(
            startDate = startDate,
            endDate = endDate,
            onStartDateChange = { startDate = it },
            onEndDateChange = { endDate = it },
            onConfirm = {
                if (startDate != null && endDate != null && startDate!! <= endDate!!) {
                    val period = Period.getCustomPeriod(startDate!!, endDate!!)
                    viewModel.loadSalesData(period)
                    showCustomPeriodDialog = false
                }
            },
            onDismiss = {
                showCustomPeriodDialog = false
                startDate = null
                endDate = null
            }
        )
    }
}
