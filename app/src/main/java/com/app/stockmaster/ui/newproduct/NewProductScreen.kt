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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.stockmaster.viewmodel.NewProductViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductScreen(
    onNavigateBack: () -> Unit,
    navController: androidx.navigation.NavHostController,
    viewModel: NewProductViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val sku by viewModel.sku.collectAsState()
    val category by viewModel.category.collectAsState()
    val costPrice by viewModel.costPrice.collectAsState()
    val salePrice by viewModel.salePrice.collectAsState()
    val initialStock by viewModel.initialStock.collectAsState()
    val minStock by viewModel.minStock.collectAsState()
    val profitMargin by viewModel.profitMargin.collectAsState()
    val imageUrl by viewModel.imageUrl.collectAsState()
    val barcode by viewModel.barcode.collectAsState()
    val isEditMode = viewModel.isEditMode

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateImageUrl(it.toString()) }
    }

    val scanResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("scan_result")
    
    LaunchedEffect(scanResult) {
        scanResult?.let {
            viewModel.updateBarcode(it)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scan_result")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            if (isEditMode) "Editar Produto" else "Novo Produto", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 20.sp 
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveProduct { onNavigateBack() } },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("SALVAR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Image Preview Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoCamera, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Sem imagem selecionada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    // Floating button to change image (concept)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Image", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionTitle("Informações Gerais")
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Nome do Produto", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("Ex: Painel Ripado WPC Amêndoa") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { viewModel.updateSku(it) },
                        label = { Text("SKU / Código", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { viewModel.updateBarcode(it) },
                        label = { Text("Código de Barras (EAN-13)", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Opcional ou gere um novo") },
                        trailingIcon = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.generateBarcode() }) {
                                    Text("GERAR", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                IconButton(onClick = { navController.navigate(com.app.stockmaster.ui.navigation.Screen.Scanner.route) }) { 
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary) 
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { viewModel.updateCategory(it) },
                        label = { Text("Categoria", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    if (!imageUrl.startsWith("data:image")) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { viewModel.updateImageUrl(it) },
                            label = { Text("URL da Imagem (Opcional)", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFF1F4F9)
                            ),
                            singleLine = true
                        )
                    } else {
                        // Display a read-only placeholder if it's base64 to avoid layout crash
                        OutlinedTextField(
                            value = "[Imagem salva no banco de dados]",
                            onValueChange = {},
                            label = { Text("URL da Imagem", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color(0xFFF1F4F9),
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionTitle("Valores e Estoque")
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { viewModel.updateCostPrice(it) },
                            label = { Text("Custo", fontWeight = FontWeight.Medium) },
                            prefix = { Text("R$ ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { viewModel.updateSalePrice(it) },
                            label = { Text("Venda", fontWeight = FontWeight.Medium) },
                            prefix = { Text("R$ ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Surface(
                        color = if (profitMargin >= 30.0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Margem de lucro estimada: ${String.format("%.1f", profitMargin)}%",
                            modifier = Modifier.padding(12.dp),
                            color = if (profitMargin >= 30.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = initialStock,
                            onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.updateInitialStock(it) },
                            label = { Text("Estoque", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.updateMinStock(it) },
                            label = { Text("Mínimo", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveProduct { onNavigateBack() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (isEditMode) "SALVAR ALTERAÇÕES" else "CRIAR PRODUTO", 
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
            ) {
                Text("Descartar e Voltar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title, 
            fontWeight = FontWeight.Black, 
            fontSize = 18.sp, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
