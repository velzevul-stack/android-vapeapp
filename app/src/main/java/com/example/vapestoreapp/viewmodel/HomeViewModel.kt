package com.example.vapestoreapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapestoreapp.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeStats(
    val soldToday: Long,
    val receiptsToday: Long,
    val revenueToday: Double,
    val profitToday: Double,
    val weekProfitByDay: List<Pair<String, Double>>
)

class HomeViewModel(context: Context) : ViewModel() {
    private val repository = Repository(context)

    private val _soldToday = MutableStateFlow(0L)
    private val _receiptsToday = MutableStateFlow(0L)
    private val _revenueToday = MutableStateFlow(0.0)
    private val _profitToday = MutableStateFlow(0.0)
    private val _weekData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())

    val homeStats: StateFlow<HomeStats> = combine(
        _soldToday,
        _receiptsToday,
        _revenueToday,
        _profitToday,
        _weekData
    ) { sold, receipts, revenue, profit, week ->
        HomeStats(
            soldToday = sold,
            receiptsToday = receipts,
            revenueToday = revenue,
            profitToday = profit,
            weekProfitByDay = week
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeStats(0L, 0L, 0.0, 0.0, emptyList())
    )

    init {
        loadTodayStats()
        loadWeekStats()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val (start, end) = getTodayRange()
            repository.getTotalQuantityInPeriod(start, end).collect { _soldToday.value = it }
        }
        viewModelScope.launch {
            val (start, end) = getTodayRange()
            repository.getSalesCountInPeriod(start, end).collect { _receiptsToday.value = it }
        }
        viewModelScope.launch {
            val (start, end) = getTodayRange()
            repository.getTotalRevenue(start, end).collect { _revenueToday.value = it ?: 0.0 }
        }
        viewModelScope.launch {
            val (start, end) = getTodayRange()
            repository.getTotalProfit(start, end).collect { _profitToday.value = it ?: 0.0 }
        }
    }

    private fun loadWeekStats() {
        viewModelScope.launch {
            val (start, end) = getLast7DaysRange()
            repository.getSalesInPeriod(start, end).collect { sales ->
                val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                val grouped = sales.filter { !it.isCancelled }
                    .groupBy { sale ->
                        Calendar.getInstance().apply {
                            timeInMillis = sale.date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                val result = mutableListOf<Pair<String, Double>>()
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                for (i in 0 until 7) {
                    val dayCal = today.clone() as Calendar
                    dayCal.add(Calendar.DAY_OF_MONTH, -6 + i)
                    val dayStart = dayCal.timeInMillis
                    val daySales = grouped[dayStart] ?: emptyList()
                    val profit = daySales.sumOf { it.profit }
                    val dayName = dayNames[(dayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7]
                    result.add(dayName to profit)
                }
                _weekData.value = result
            }
        }
    }

    fun refresh() {
        loadTodayStats()
        loadWeekStats()
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        return Pair(start, end)
    }

    private fun getLast7DaysRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, -7)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return Pair(start, end)
    }
}
