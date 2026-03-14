package com.app.stockmaster.ui.newproduct

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.stockmaster.viewmodel.NewProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewProductViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val sku by viewModel.sku.collectAsState()
    val category by viewModel.category.collectAsState()
    val costPrice by viewModel.costPrice.collectAsState()
    val salePrice by viewModel.salePrice.collectAsState()
    val initialStock by viewModel.initialStock.collectAsState()
    val minStock by viewModel.minStock.collectAsState()
    val isEditMode = viewModel.isEditMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Produto" else "Novo Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveProduct { onNavigateBack() } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... (rest of the card and text fields)
            // No changes needed inside the Column except the final button

            // (Skipping ahead to the final button at the end)
        }
    }
}
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF5F7FA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Insira uma URL de imagem abaixo", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { viewModel.updateImageUrl(it) },
                label = { Text("URL da Foto") },
                placeholder = { Text("https://exemplo.com/foto.jpg") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            // General Information
            SectionTitle("Informações Gerais")
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Nome do Produto") },
                placeholder = { Text("Ex: Painel Ripado WPC Amêndoa") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = sku,
                onValueChange = { viewModel.updateSku(it) },
                label = { Text("SKU / Código") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = {}) { Icon(Icons.Default.QrCodeScanner, contentDescription = null) } },
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = category,
                onValueChange = { viewModel.updateCategory(it) },
                label = { Text("Categoria") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            // Pricing
            SectionTitle("Precificação")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { viewModel.updateCostPrice(it) },
                    label = { Text("Preço de Custo") },
                    prefix = { Text("R$ ") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = salePrice,
                    onValueChange = { viewModel.updateSalePrice(it) },
                    label = { Text("Preço de Venda") },
                    prefix = { Text("R$ ") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Text(
                "Margem de lucro estimada: ${String.format("%.1f", profitMargin)}%",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Stock
            SectionTitle("Estoque")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = initialStock,
                    onValueChange = { viewModel.updateInitialStock(it) },
                    label = { Text("Estoque Inicial") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = minStock,
                    onValueChange = { viewModel.updateMinStock(it) },
                    label = { Text("Alerta de Mínimo") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveProduct { onNavigateBack() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isEditMode) "Salvar Alterações" else "Salvar Novo Produto", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancelar e Sair", color = Color.Gray)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 4.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}
