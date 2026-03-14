package com.app.stockmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.local.entity.CategoryEntity
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.repository.CategoryRepository
import com.app.stockmaster.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        itemRepository.getAllItems()
    ) { query, category, allItems ->
        allItems.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true) || 
                               item.sku.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
}
