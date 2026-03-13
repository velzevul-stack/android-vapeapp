package com.example.vapestoreapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vapestoreapp.ui.components.DangerButton
import com.example.vapestoreapp.ui.components.SectionCard
import com.example.vapestoreapp.ui.components.TonalButton
import com.example.vapestoreapp.ui.theme.semanticColors
import com.example.vapestoreapp.viewmodel.CabinetViewModel

@Composable
fun SettingsScreen(
    viewModel: CabinetViewModel,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val backupResult by viewModel.backupResult.collectAsState()

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromUri(it) }
    }

    var showClearStockDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = "Тема",
            subtitle = "Переключение режима отображения",
            icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тёмная тема", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }
        }

        SectionCard(
            title = "База данных",
            subtitle = "Резервная копия и восстановление",
            icon = Icons.Default.Storage
        ) {
            TonalButton(
                onClick = { viewModel.exportDatabase() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Экспорт")
            }

            Spacer(modifier = Modifier.height(10.dp))

            TonalButton(
                onClick = { importLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Импорт")
            }
        }

        SectionCard(
            title = "Опасная зона",
            subtitle = "Действия, которые нельзя отменить",
            icon = Icons.Default.Warning
        ) {
            DangerButton(
                onClick = { showClearStockDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Очистить весь склад")
            }
        }

        // Result message
        if (backupResult.isNotEmpty()) {
            val semantic = MaterialTheme.semanticColors
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = semantic.infoContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = semantic.onInfoContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            backupResult,
                            color = semantic.onInfoContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = { viewModel.clearBackupResult() }) {
                        Text("OK", color = semantic.onInfoContainer)
                    }
                }
            }
        }
    }

    if (showClearStockDialog) {
        AlertDialog(
            onDismissRequest = { showClearStockDialog = false },
            title = { Text("Очистить склад?") },
            text = { Text("Вы уверены, что хотите удалить ВСЕ товары со склада? Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllStock()
                        showClearStockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStockDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
