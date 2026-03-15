package com.app.stockmaster.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.local.entity.TransactionEntity
import com.app.stockmaster.data.repository.ItemRepository
import com.app.stockmaster.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockAdjustmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val itemId: Int = checkNotNull(savedStateHandle["itemId"])

    val item: StateFlow<ItemEntity?> = itemRepository.getAllItems()
        .map { items -> items.find { it.id == itemId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _quantity = MutableStateFlow("")
    val quantity: StateFlow<String> = _quantity.asStateFlow()

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    private val _isAddition = MutableStateFlow(true)
    val isAddition: StateFlow<Boolean> = _isAddition.asStateFlow()

    fun updateQuantity(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _quantity.value = value
        }
    }

    fun updateReason(value: String) {
        _reason.value = value
    }

    fun setType(isAddition: Boolean) {
        _isAddition.value = isAddition
    }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun confirmTransaction(onSuccess: () -> Unit) {
        val qty = _quantity.value.toIntOrNull() ?: return
        if (qty <= 0) return
        if (_reason.value.isEmpty()) return

        _isSaving.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val transaction = TransactionEntity(
                    itemId = itemId,
                    type = if (_isAddition.value) "INBOUND" else "OUTBOUND",
                    quantity = qty,
                    reason = _reason.value,
                    date = System.currentTimeMillis()
                )
                transactionRepository.addTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("StockAdjVM", "Error confirming transaction", e)
                _error.value = "Erro ao atualizar estoque: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
