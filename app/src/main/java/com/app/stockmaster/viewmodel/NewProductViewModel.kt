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
class NewProductViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: Int? = savedStateHandle["itemId"]
    val isEditMode: Boolean = itemId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    init {
        itemId?.let { id ->
            viewModelScope.launch {
                itemRepository.getItemById(id)?.let { item ->
                    _name.value = item.name
                    _sku.value = item.sku
                    _category.value = item.category
                    _costPrice.value = item.costPrice.toString()
                    _salePrice.value = item.salePrice.toString()
                    _initialStock.value = item.currentStock.toString()
                    _minStock.value = item.minStockAlert.toString()
                    _imageUrl.value = item.imageUri ?: ""
                }
            }
        }
    }

    private val _sku = MutableStateFlow("")
    val sku: StateFlow<String> = _sku.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _costPrice = MutableStateFlow("")
    val costPrice: StateFlow<String> = _costPrice.asStateFlow()

    private val _salePrice = MutableStateFlow("")
    val salePrice: StateFlow<String> = _salePrice.asStateFlow()

    private val _initialStock = MutableStateFlow("")
    val initialStock: StateFlow<String> = _initialStock.asStateFlow()

    private val _minStock = MutableStateFlow("5")
    val minStock: StateFlow<String> = _minStock.asStateFlow()

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> = _imageUrl.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profitMargin: StateFlow<Double> = combine(_costPrice, _salePrice) { cost, sale ->
        val c = cost.toDoubleOrNull() ?: 0.0
        val s = sale.toDoubleOrNull() ?: 0.0
        if (c > 0) ((s - c) / c) * 100 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateName(value: String) { _name.value = value }
    fun updateSku(value: String) { _sku.value = value }
    fun updateCategory(value: String) { _category.value = value }
    fun updateCostPrice(value: String) { _costPrice.value = value }
    fun updateSalePrice(value: String) { _salePrice.value = value }
    fun updateInitialStock(value: String) { _initialStock.value = value }
    fun updateMinStock(value: String) { _minStock.value = value }
    fun updateImageUrl(value: String) { _imageUrl.value = value }

    fun saveProduct(onSuccess: () -> Unit) {
        if (_name.value.isEmpty() || _sku.value.isEmpty()) return

        viewModelScope.launch {
            val item = ItemEntity(
                name = _name.value,
                sku = _sku.value,
                barcode = _sku.value, 
                category = _category.value,
                costPrice = _costPrice.value.toDoubleOrNull() ?: 0.0,
                salePrice = _salePrice.value.toDoubleOrNull() ?: 0.0,
                currentStock = _initialStock.value.toIntOrNull() ?: 0,
                minStockAlert = _minStock.value.toIntOrNull() ?: 5,
                imageUri = _imageUrl.value.ifBlank { null },
                tinyId = itemId?.let { id -> itemRepository.getItemById(id)?.tinyId }
            )

            if (isEditMode) {
                val updatedItem = item.copy(id = itemId!!, updatedAt = System.currentTimeMillis())
                itemRepository.updateItem(updatedItem)
                // Also update remotely if it has a tinyId
                updatedItem.tinyId?.let { remoteId ->
                    itemRepository.updateRemoteProduct(updatedItem)
                }
            } else {
                val localId = itemRepository.insertItem(item)
                itemRepository.addRemoteProduct(item.copy(id = localId.toInt()))
            }
            onSuccess()
        }
    }
}
