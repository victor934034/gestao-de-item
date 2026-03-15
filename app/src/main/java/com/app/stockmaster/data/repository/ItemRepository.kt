package com.app.stockmaster.data.repository

import com.app.stockmaster.data.local.dao.ItemDao
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.remote.BridgeApi
import com.app.stockmaster.data.remote.BridgeProduct
import com.app.stockmaster.data.remote.ErpApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.app.stockmaster.data.remote.BridgeProductResponse
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: com.app.stockmaster.data.local.dao.ItemDao,
    private val erpApi: com.app.stockmaster.data.remote.ErpApi,
    private val bridgeApi: com.app.stockmaster.data.remote.BridgeApi,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            
            // Create a temp file to upload
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            
            val response = bridgeApi.uploadImage(body)
            tempFile.delete() // Clean up
            
            if (response.isSuccessful) {
                val imageUrl = response.body()?.get("imageUrl") as? String
                android.util.Log.d("Upload", "Image uploaded successfully: $imageUrl")
                imageUrl
            } else {
                android.util.Log.e("Upload", "Image upload failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("Upload", "Exception during image upload", e)
            null
        }
    }
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()
    
    fun getLowStockItems(): Flow<List<ItemEntity>> = itemDao.getLowStockItems()
    
    fun getTotalItemCount(): Flow<Int> = itemDao.getTotalItemCount()
    
    fun searchItems(query: String): Flow<List<ItemEntity>> = itemDao.searchItems(query)
    
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)
    
    suspend fun updateItem(item: ItemEntity) = itemDao.updateItem(item)
    
    suspend fun deleteItem(id: Int) = itemDao.deleteItem(id)

    suspend fun getItemById(id: Int): ItemEntity? = itemDao.getItemById(id)

    suspend fun getItemByBarcode(barcode: String): ItemEntity? = itemDao.getItemByBarcode(barcode)

    suspend fun syncWithBridge(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("Sync", "Starting bidirectional sync...")
            
            // 1. Push local changes first
            pushLocalItemsToRemote()
            
            // 2. Pull remote changes
            val response = bridgeApi.getProducts()
            android.util.Log.d("Sync", "Supabase Pull Response Code: ${response.code()}")
            
            if (response.isSuccessful) {
                val products = response.body()?.products ?: emptyList()
                android.util.Log.d("Sync", "Received ${products.size} products from Unified Backend")
                products.forEach { bp ->
                    // Convert Any id to a clean string (no .0)
                    val rawId = bp.id
                    val remoteIdStr = when (rawId) {
                        is Double -> rawId.toLong().toString()
                        is Float -> rawId.toLong().toString()
                        else -> rawId.toString()
                    }

                    val existing = itemDao.getItemByTinyId(remoteIdStr) 
                        ?: itemDao.getItemBySku(remoteIdStr) 
                    
                    val entity = ItemEntity(
                        id = existing?.id ?: 0,
                        name = bp.nome,
                        sku = remoteIdStr,
                        barcode = bp.barcode ?: existing?.barcode ?: "", 
                        category = bp.modo_estocagem ?: "Geral",
                        costPrice = bp.custo ?: 0.0,
                        salePrice = bp.preco_venda ?: 0.0,
                        currentStock = bp.quantidade.toInt(),
                        minStockAlert = (existing?.minStockAlert ?: 5),
                        tinyId = remoteIdStr, 
                        imageUri = bp.foto_url,
                        updatedAt = System.currentTimeMillis()
                    )
                    itemDao.insertItem(entity)
                }
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Sem corpo de erro"
                android.util.Log.e("Sync", "Supabase Pull Error Response (${response.code()}): $errorBody")
                Result.failure(Exception("Erro na Supabase (Pull): ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Sync Exception", e)
            Result.failure(e)
        }
    }

    suspend fun pushLocalItemsToRemote() = withContext(Dispatchers.IO) {
        try {
            val unsynced = itemDao.getUnsyncedItems()
            android.util.Log.d("Sync", "Found ${unsynced.size} unsynced items to push")
            unsynced.forEach { item ->
                addRemoteProduct(item)
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Error during push sync", e)
        }
    }

    suspend fun updateRemoteStock(itemId: Int, newQuantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = itemDao.getItemById(itemId) ?: return@withContext Result.failure(Exception("Item não encontrado"))
            val remoteId = item.tinyId 
            
            if (remoteId == null) {
                android.util.Log.w("Sync", "Item $itemId missing remote ID, attempting to add first")
                val addResult = addRemoteProduct(item)
                if (addResult.isFailure) return@withContext addResult
                // Re-fetch to get new remote ID
                return@withContext updateRemoteStock(itemId, newQuantity)
            }
            
            val response = bridgeApi.updateQuantity(remoteId, mapOf("quantidade" to newQuantity))
            if (response.isSuccessful) {
                android.util.Log.d("Sync", "Remote stock update successful for item $remoteId")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("Sync", "Remote stock update failed (${response.code()}): $errorBody")
                Result.failure(Exception("Erro ao atualizar estoque no Backend: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Exception updating remote stock", e)
            Result.failure(e)
        }
    }

    suspend fun addRemoteProduct(item: ItemEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bp = BridgeProduct(
                id = item.id,
                nome = item.name,
                quantidade = item.currentStock.toDouble(),
                modo_estocagem = item.category,
                custo = item.costPrice,
                preco_venda = item.salePrice,
                foto_url = item.imageUri,
                barcode = item.barcode
            )
            val response = bridgeApi.addProduct(bp)
            if (response.isSuccessful) {
                // Update local item with the remote ID if returned
                val remoteProduct = response.body()?.product
                if (remoteProduct != null) {
                    android.util.Log.d("Sync", "Product added remotely, sync ID: ${remoteProduct.id}")
                    itemDao.insertItem(item.copy(tinyId = remoteProduct.id.toString()))
                }
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("Sync", "Add remote product failed (${response.code()}): $errorBody")
                Result.failure(Exception("Erro ao adicionar produto no Backend: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Exception adding remote product", e)
            Result.failure(e)
        }
    }

    suspend fun updateRemoteProduct(item: ItemEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteId = item.tinyId 
            if (remoteId == null) {
                android.util.Log.w("Sync", "Item ${item.id} missing remote ID during update, adding instead")
                return@withContext addRemoteProduct(item)
            }
            
            val bp = BridgeProduct(
                id = item.id, 
                nome = item.name,
                quantidade = item.currentStock.toDouble(),
                modo_estocagem = item.category,
                custo = item.costPrice,
                preco_venda = item.salePrice,
                foto_url = item.imageUri,
                barcode = item.barcode
            )
            val response = bridgeApi.updateProduct(remoteId, bp)
            if (response.isSuccessful) {
                android.util.Log.d("Sync", "Remote product update successful for ID: $remoteId")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("Sync", "Remote product update failed (${response.code()}): $errorBody")
                Result.failure(Exception("Erro ao atualizar produto no Backend: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Exception updating remote product", e)
            Result.failure(e)
        }
    }

    suspend fun syncWithTiny(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var currentPage = 1
            var totalPages = 1
            
            do {
                val response = erpApi.searchProducts(page = currentPage)
                if (response.isSuccessful) {
                    val searchReturn = response.body()?.retorno
                    if (searchReturn?.status == "OK") {
                        totalPages = searchReturn.numero_paginas ?: 1
                        
                        searchReturn.produtos?.forEach { wrapped ->
                            val tp = wrapped.produto
                            
                            // Fetch detailed stock for the product
                            val stockResponse = erpApi.getProductStock(tp.id)
                            val stock = if (stockResponse.isSuccessful) {
                                (stockResponse.body()?.retorno?.produto?.saldo ?: stockResponse.body()?.retorno?.produto?.estoque_atual ?: 0.0).toInt()
                            } else 0

                            val existing = itemDao.getItemByTinyId(tp.id) 
                                ?: if (!tp.codigo.isNullOrBlank()) itemDao.getItemBySku(tp.codigo) else null
                            
                            val entity = ItemEntity(
                                id = existing?.id ?: 0,
                                name = tp.nome,
                                sku = tp.codigo ?: "",
                                barcode = existing?.barcode ?: "", 
                                category = "Geral",
                                costPrice = 0.0,
                                salePrice = tp.preco ?: tp.preco_promocional ?: 0.0,
                                currentStock = stock,
                                minStockAlert = 5,
                                tinyId = tp.id,
                                updatedAt = System.currentTimeMillis()
                            )
                            itemDao.insertItem(entity)
                        }
                        currentPage++
                    } else {
                        val error = searchReturn?.erros?.firstOrNull()?.erro ?: "Erro desconhecido na API do Tiny"
                        return@withContext Result.failure(Exception(error))
                    }
                } else {
                    return@withContext Result.failure(Exception("Erro na API: ${response.code()}"))
                }
            } while (currentPage <= totalPages)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
