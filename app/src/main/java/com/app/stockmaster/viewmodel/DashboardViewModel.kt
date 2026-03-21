package com.app.stockmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.local.model.DayActivity
import com.app.stockmaster.data.local.model.TransactionWithItem
import com.app.stockmaster.data.repository.ItemRepository
import com.app.stockmaster.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val totalItems: StateFlow<Int> = itemRepository.getTotalItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockCount: StateFlow<Int> = itemRepository.getLowStockItems()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)



    private val startOfDay: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val recentMovements: StateFlow<Int> = transactionRepository.getTodayMovements(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val weekStart: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
    }.timeInMillis

    val weeklyActivity: StateFlow<List<DayActivity>> = transactionRepository.getWeeklyActivity(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestTransactions: StateFlow<List<TransactionWithItem>> = transactionRepository.getRecentTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        triggerSync()
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncing.value = true
            _syncError.value = null
            itemRepository.syncWithBridge()
                .onFailure { e ->
                    _syncError.value = e.message
                }
            
            transactionRepository.syncWithBridge()
                .onFailure { e ->
                    // Optionally handle transaction sync error separately
                    if (_syncError.value == null) _syncError.value = e.message
                }
            _syncing.value = false
        }
    }
}
