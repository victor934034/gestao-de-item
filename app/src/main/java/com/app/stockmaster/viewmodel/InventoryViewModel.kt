package com.app.stockmaster.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.local.entity.CategoryEntity
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.repository.CategoryRepository
import com.app.stockmaster.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filter: String? = savedStateHandle["filter"]
    private val _showLowStockOnly = MutableStateFlow(filter == "low_stock")
    val showLowStockOnly = _showLowStockOnly.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        _showLowStockOnly,
        itemRepository.getAllItems()
    ) { query, category, lowStockOnly, allItems ->
        allItems.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true) || 
                               item.sku.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            val matchesLowStock = !lowStockOnly || item.currentStock <= item.minStockAlert
            matchesQuery && matchesCategory && matchesLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleLowStockOnly(value: Boolean) {
        _showLowStockOnly.value = value
    }

    val lowStockCount: StateFlow<Int> = itemRepository.getAllItems()
        .map { items -> items.count { it.currentStock <= it.minStockAlert } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun syncWithBridge() {
        viewModelScope.launch {
            itemRepository.syncWithBridge()
        }
    }

    suspend fun findItemByBarcode(barcode: String): ItemEntity? {
        return itemRepository.getItemByBarcode(barcode)
    }
}
