package com.app.stockmaster.ui.adjustment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.stockmaster.viewmodel.StockAdjustmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockAdjustmentViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val reason by viewModel.reason.collectAsState()
    val isAddition by viewModel.isAddition.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val reasons = listOf("Compra / Recebimento", "Venda / Saída", "Devolução", "Avaria / Perda", "Inventário", "Outros")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajuste de Estoque", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        item?.let { currentItem ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Header Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = currentItem.imageUri ?: "https://via.placeholder.com/80",
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(currentItem.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${currentItem.currentStock} unidades • SKU: ${currentItem.sku}", color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "EM ESTOQUE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = Color(0xFF4CAF50),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Toggle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdjustmentTypeButton(
                        modifier = Modifier.weight(1f),
                        text = "ENTRADA",
                        icon = Icons.Default.AddCircle,
                        selected = isAddition,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.setType(true) }
                    )
                    AdjustmentTypeButton(
                        modifier = Modifier.weight(1f),
                        text = "SAÍDA",
                        icon = Icons.Default.RemoveCircle,
                        selected = !isAddition,
                        selectedColor = Color.LightGray,
                        onClick = { viewModel.setType(false) }
                    )
                }

                Text("Detalhes da Transação", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { viewModel.updateQuantity(it) },
                    label = { Text("Quantidade") },
                    suffix = { Text("unidades") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Box {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {},
                        label = { Text("Motivo do Ajuste") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        reasons.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    viewModel.updateReason(r)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = "", // TODO: Date implementation
                    onValueChange = {},
                    label = { Text("Data da Transação") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true
                )

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Observação (Opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.confirmTransaction { onNavigateBack() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Confirmar Transação", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdjustmentTypeButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) selectedColor else Color(0xFFF5F7FA)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text,
                color = if (selected) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
