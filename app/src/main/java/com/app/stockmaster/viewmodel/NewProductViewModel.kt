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

import android.util.Log

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

    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode.asStateFlow()

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
                    _barcode.value = item.barcode
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
    fun updateBarcode(value: String) { _barcode.value = value }
    fun updateImageUrl(value: String) { _imageUrl.value = value }

    fun generateBarcode() {
        _barcode.value = com.app.stockmaster.util.Ean13Generator.generate()
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveProduct(onSuccess: () -> Unit) {
        if (_name.value.isEmpty() || _sku.value.isEmpty()) {
            _error.value = "Nome e SKU são obrigatórios"
            return
        }

        // Ensure we have a barcode. If blank, generate a valid EAN-13
        if (_barcode.value.isBlank()) {
            generateBarcode()
        }

        _isSaving.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Upload image if it's a local URI
                var finalImageUrl = _imageUrl.value.ifBlank { null }
                if (finalImageUrl != null && finalImageUrl.startsWith("content://")) {
                    android.util.Log.d("NewProductVM", "Local URI detected, uploading: $finalImageUrl")
                    val cloudUrl = itemRepository.uploadImage(android.net.Uri.parse(finalImageUrl))
                    if (cloudUrl != null) {
                        finalImageUrl = cloudUrl
                        _imageUrl.value = cloudUrl // Update UI state too
                    } else {
                        Log.e("NewProductVM", "Image upload failed, falling back to local URI")
                    }
                }

                val item = ItemEntity(
                    name = _name.value,
                    sku = _sku.value,
                    barcode = _barcode.value, 
                    category = _category.value,
                    costPrice = _costPrice.value.toDoubleOrNull() ?: 0.0,
                    salePrice = _salePrice.value.toDoubleOrNull() ?: 0.0,
                    currentStock = _initialStock.value.toIntOrNull() ?: 0,
                    minStockAlert = _minStock.value.toIntOrNull() ?: 5,
                    imageUri = finalImageUrl,
                    tinyId = itemId?.let { id -> itemRepository.getItemById(id)?.tinyId },
                    updatedAt = System.currentTimeMillis()
                )

                if (isEditMode) {
                    val updatedItem = item.copy(id = itemId!!, updatedAt = System.currentTimeMillis())
                    itemRepository.updateItem(updatedItem)
                    // Also update remotely if it has a tinyId
                    updatedItem.tinyId?.let { remoteId ->
                        Log.d("NewProductVM", "Attempting remote update for $remoteId")
                        val result = itemRepository.updateRemoteProduct(updatedItem)
                        result.onFailure { e ->
                            Log.e("NewProductVM", "Remote update failed", e)
                            // We don't block success of local save, but we log it
                        }
                    }
                } else {
                    val localId = itemRepository.insertItem(item)
                    Log.d("NewProductVM", "Attempting to add remote product for local ID: $localId")
                    val result = itemRepository.addRemoteProduct(item.copy(id = localId.toInt()))
                    result.onFailure { e ->
                        Log.e("NewProductVM", "Remote add failed", e)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("NewProductVM", "General save error", e)
                _error.value = "Erro ao salvar: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
