package com.app.stockmaster.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stockmaster.data.repository.ItemRepository
import com.app.stockmaster.service.DataManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val dataService: DataManagementService
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("Exportando dados...")
            val items = repository.getAllItems().first()
            val result = dataService.exportToCsv(uri, items)
            _uiState.value = if (result.isSuccess) {
                SettingsUiState.Success("Dados exportados com sucesso!")
            } else {
                SettingsUiState.Error("Erro ao exportar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("Importando dados...")
            val result = dataService.importFromCsv(uri)
            if (result.isSuccess) {
                val items = result.getOrNull() ?: emptyList()
                items.forEach { repository.insertItem(it) }
                _uiState.value = SettingsUiState.Success("${items.size} itens importados com sucesso!")
            } else {
                _uiState.value = SettingsUiState.Error("Erro ao importar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun syncBackup() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("Sincronizando com a nuvem...")
            val result = repository.syncWithBridge()
            _uiState.value = if (result.isSuccess) {
                SettingsUiState.Success("Sincronização concluída!")
            } else {
                SettingsUiState.Error("Erro na sincronização: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = SettingsUiState.Idle
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    data class Loading(val message: String) : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
