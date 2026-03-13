package com.example.vapestoreapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapestoreapp.data.Debt
import com.example.vapestoreapp.data.Product
import com.example.vapestoreapp.ui.components.ScreenScaffold
import com.example.vapestoreapp.ui.components.OperationResultSnackbar
import com.example.vapestoreapp.viewmodel.DebtsViewModel
import com.example.vapestoreapp.viewmodel.DebtsViewModelFactory

@Composable
fun DebtsScreen() {
    val context = LocalContext.current
    val viewModel: DebtsViewModel = viewModel(factory = DebtsViewModelFactory(context))
    
    val debts by viewModel.debts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    OperationResultSnackbar(
        operationResult = operationResult,
        onClear = { viewModel.clearResult() },
        snackbarHostState = snackbarHostState
    )

    ScreenScaffold(
        title = "Долги",
        snackbarHostState = snackbarHostState,
        onRefresh = { viewModel.refresh() },
        actions = {
            IconButton(onClick = { viewModel.showCreateDebtDialog() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (debts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет активных долгов",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(debts, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDebtDialog(
            allProducts = allProducts,
            onDismiss = { viewModel.hideCreateDebtDialog() },
            onCreate = { customerName, items, totalAmount ->
                viewModel.createDebt(customerName, items, totalAmount)
            }
        )
    }
}

@Composable
private fun CreateDebtDialog(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onCreate: (customerName: String, items: List<Pair<Product, Int>>, totalAmount: Double) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var selectedProductId by remember(allProducts) {
        mutableStateOf(allProducts.firstOrNull()?.id)
    }
    var qtyText by remember { mutableStateOf("1") }
    var totalAmountText by remember { mutableStateOf("") }
    var manualTotal by remember { mutableStateOf(false) }

    val selectedProduct = remember(allProducts, selectedProductId) {
        allProducts.firstOrNull { it.id == selectedProductId }
    }

    val items = remember { mutableStateListOf<Pair<Product, Int>>() }

    val computedTotal = remember(items) {
        items.sumOf { (p, q) -> p.retailPrice * q }
    }

    LaunchedEffect(computedTotal, manualTotal) {
        if (!manualTotal) {
            totalAmountText = if (computedTotal == 0.0) "" else String.format("%.2f", computedTotal)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val total = totalAmountText.trim().replace(",", ".").toDoubleOrNull() ?: computedTotal
                    onCreate(customerName, items.toList(), total)
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Добавить долг") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Имя клиента") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (allProducts.isEmpty()) {
                    Text(
                        text = "Нет товаров для выбора",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Товары",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ProductPickerRow(
                            allProducts = allProducts,
                            selectedProductId = selectedProductId,
                            onSelectedProductIdChange = { selectedProductId = it },
                            qtyText = qtyText,
                            onQtyTextChange = { qtyText = it },
                            onAdd = {
                                val p = selectedProduct ?: return@ProductPickerRow
                                val q = qtyText.trim().toIntOrNull() ?: 0
                                if (q <= 0) return@ProductPickerRow
                                items.add(p to q)
                                qtyText = "1"
                            }
                        )

                        if (items.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items.forEachIndexed { index, (p, q) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p.brand, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${p.flavor} • x$q",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { items.removeAt(index) }) {
                                            Icon(Icons.Default.Remove, contentDescription = "Убрать")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = {
                        manualTotal = true
                        totalAmountText = it
                    },
                    label = { Text("Сумма долга (BYN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (!manualTotal) {
                            Text("Подсчитано автоматически")
                        } else {
                            TextButton(onClick = { manualTotal = false }) { Text("Сбросить к авто-сумме") }
                        }
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerRow(
    allProducts: List<Product>,
    selectedProductId: Int?,
    onSelectedProductIdChange: (Int) -> Unit,
    qtyText: String,
    onQtyTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = allProducts.firstOrNull { it.id == selectedProductId } ?: allProducts.first()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = "${selected.brand} • ${selected.flavor}",
                onValueChange = {},
                label = { Text("Товар") },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                allProducts.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.brand} • ${p.flavor}") },
                        onClick = {
                            onSelectedProductIdChange(p.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = qtyText,
                onValueChange = onQtyTextChange,
                label = { Text("Кол-во") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAdd, modifier = Modifier.height(56.dp)) {
                Text("Добавить")
            }
        }
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    viewModel: DebtsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = debt.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f BYN".format(debt.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = viewModel.formatDebtProducts(debt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = viewModel.formatDate(debt.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.payDebt(debt.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Погасить")
                }
            }
        }
    }
}
