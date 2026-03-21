package com.app.stockmaster.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.stockmaster.viewmodel.SettingsViewModel
import com.app.stockmaster.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Activity Result Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    // Handle UI State changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsCategory("Geral")
                SettingsItem("Perfil da Empresa", Icons.Default.Business, "Logo, endereço e nome") {
                    // Navigate to profile edit (to be implemented)
                }
                SettingsItem("Notificações", Icons.Default.Notifications, "Limites de alerta de estoque") {
                    // Navigate to notification settings
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategory("Gerenciamento de Dados")
                SettingsItem("Exportar Dados", Icons.Default.FileDownload, "Exportar como CSV") {
                    exportLauncher.launch("estoque_backup_${System.currentTimeMillis()}.csv")
                }
                SettingsItem("Importar Dados", Icons.Default.FileUpload, "Importar itens de um CSV") {
                    importLauncher.launch("text/*")
                }
                SettingsItem("Backup na Nuvem", Icons.Default.CloudSync, "Sincronizar com Bridge/Supabase") {
                    viewModel.syncBackup()
                }

                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategory("Aparência")
                SettingsItem("Tema", Icons.Default.Palette, "Modo Escuro / Modo Claro") {
                    // Toggle theme logic
                }
                SettingsItem("Idioma", Icons.Default.Language, "Português / English") { }

                Spacer(modifier = Modifier.height(16.dp))
                SettingsCategory("Sobre")
                SettingsItem("Versão do App", Icons.Default.Info, "v1.0.2 (Build 002)") { }
                SettingsItem("Licenças", Icons.Default.CardMembership, "Bibliotecas open source") { }
            }

            if (uiState is SettingsUiState.Loading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            (uiState as SettingsUiState.Loading).message,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
