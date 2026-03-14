package com.app.stockmaster.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.stockmaster.data.local.model.TransactionWithItem
import com.app.stockmaster.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val totalItems by viewModel.totalItems.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val recentMovements by viewModel.recentMovements.collectAsState()
    val latestTransactions by viewModel.latestTransactions.collectAsState()
    val isSyncing by viewModel.syncing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONEXÃO BR 277", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.triggerSync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync with Tiny")
                        }
                    }
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Resumo do Estoque",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Última atualização: Hoje, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            item {
                SummaryCard(
                    title = "Total de Itens",
                    value = totalItems.toString(),
                    subValue = "Contagem total no estoque",
                    icon = Icons.Default.Inventory,
                    iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Alertas Baixos",
                        value = lowStockCount.toString(),
                        icon = Icons.Default.Warning,
                        iconColor = Color.Red,
                        isUrgent = lowStockCount > 0
                    )
                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Movimentos Hoje",
                        value = "+$recentMovements",
                        icon = Icons.Default.SwapHoriz,
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Text(
                    "Últimas Transações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(latestTransactions) { transaction ->
                TransactionItem(transaction)
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    iconBackgroundColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(subValue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SmallSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isUrgent: Boolean = false
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isUrgent) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
                if (isUrgent) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "URGENT",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionWithItem) {
    val icon = when (transaction.type) {
        "INBOUND" -> Icons.Default.Add
        "OUTBOUND" -> Icons.Default.Remove
        else -> Icons.Default.Info
    }
    val iconColor = when (transaction.type) {
        "INBOUND" -> Color(0xFFE8F5E9)
        "OUTBOUND" -> Color(0xFFE3F2FD)
        else -> Color(0xFFFFEBEE)
    }
    val tintColor = when (transaction.type) {
        "INBOUND" -> Color(0xFF4CAF50)
        "OUTBOUND" -> Color(0xFF2196F3)
        else -> Color(0xFFFF5252)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tintColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.itemName, fontWeight = FontWeight.Bold)
            Text(
                "${transaction.type.lowercase().replaceFirstChar { it.uppercase() }} • ${transaction.quantity} units",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            "2m ago", // TODO: Format relative time
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
