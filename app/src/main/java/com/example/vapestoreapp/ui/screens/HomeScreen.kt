package com.example.vapestoreapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapestoreapp.ui.components.KPICard
import com.example.vapestoreapp.ui.components.KPICardColor
import com.example.vapestoreapp.ui.theme.PastelBlue
import com.example.vapestoreapp.ui.theme.PastelLavender
import com.example.vapestoreapp.ui.theme.PastelMint
import com.example.vapestoreapp.ui.theme.PastelPink
import com.example.vapestoreapp.ui.theme.PastelTeal
import com.example.vapestoreapp.ui.theme.SurfaceDark
import com.example.vapestoreapp.ui.theme.SurfaceElevatedDark
import com.example.vapestoreapp.ui.theme.TextSecondaryDark
import com.example.vapestoreapp.viewmodel.HomeViewModel
import com.example.vapestoreapp.viewmodel.HomeViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
    val stats by viewModel.homeStats.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 32.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Добро пожаловать",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "VapeStore Dashboard",
                        fontSize = 14.sp,
                        color = TextSecondaryDark
                    )
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = TextSecondaryDark
                    )
                }
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    KPICard(
                        title = "Продано сегодня",
                        value = stats.soldToday.toString(),
                        icon = Icons.Default.ShoppingBag,
                        color = KPICardColor.MINT
                    )
                }
                item {
                    KPICard(
                        title = "Чеков сегодня",
                        value = stats.receiptsToday.toString(),
                        icon = Icons.Default.Receipt,
                        color = KPICardColor.BLUE
                    )
                }
                item {
                    KPICard(
                        title = "Выручка",
                        value = formatMoney(stats.revenueToday),
                        icon = Icons.Default.TrendingUp,
                        color = KPICardColor.LAVENDER
                    )
                }
                item {
                    KPICard(
                        title = "Прибыль",
                        value = formatMoney(stats.profitToday),
                        icon = Icons.Default.Wallet,
                        color = KPICardColor.PINK
                    )
                }
            }
        }

        item {
            ChartCard(weekData = stats.weekProfitByDay)
        }

        item {
            QuickActionsCard(navController = navController)
        }
    }
}

@Composable
private fun ChartCard(
    weekData: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxProfit = weekData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Прибыль за неделю",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatMoney(weekData.sumOf { it.second }),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEachIndexed { index, (dayName, profit) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val barHeight = if (maxProfit > 0) (profit / maxProfit * 80).dp else 0.dp
                        val isMax = profit >= maxProfit && profit > 0
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(24.dp)
                                .height(barHeight.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isMax) PastelTeal
                                    else listOf(PastelMint, PastelBlue, PastelLavender, PastelPink, PastelMint, PastelTeal, PastelBlue)[index % 7]
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PastelMint)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обычный день", fontSize = 12.sp, color = TextSecondaryDark)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PastelTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Лучший день", fontSize = 12.sp, color = TextSecondaryDark)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple("Новая продажа", PastelMint, "sell"),
        Triple("Приемка товара", PastelBlue, "accept"),
        Triple("Отчеты", PastelLavender, "cabinet"),
        Triple("Инвентаризация", PastelPink, "cabinet")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Быстрые действия",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.chunked(2).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { (label, accentColor, route) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceElevatedDark)
                                    .clickable { navController.navigate(route) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun formatMoney(value: Double): String {
    return when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000 -> "%.0fk".format(value / 1_000)
        else -> "%.0f".format(value)
    }
}
