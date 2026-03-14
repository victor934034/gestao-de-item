package com.app.stockmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.repository.ItemRepository
import com.app.stockmaster.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val stockHealth: StateFlow<StockHealthData> = itemRepository.getAllItems()
        .map { items ->
            val total = items.size.coerceAtLeast(1)
            val inStock = items.count { it.currentStock > it.minStockAlert }
            val lowStock = items.count { it.currentStock in 1..it.minStockAlert }
            val outOfStock = items.count { it.currentStock <= 0 }
            
            StockHealthData(
                inStockPercent = (inStock.toFloat() / total) * 100,
                lowStockPercent = (lowStock.toFloat() / total) * 100,
                outOfStockPercent = (outOfStock.toFloat() / total) * 100
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockHealthData())
}

data class StockHealthData(
    val inStockPercent: Float = 0f,
    val lowStockPercent: Float = 0f,
    val outOfStockPercent: Float = 0f
)
