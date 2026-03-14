package com.app.stockmaster.data.repository

import com.app.stockmaster.data.local.dao.ItemDao
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.remote.BridgeApi
import com.app.stockmaster.data.remote.BridgeProduct
import com.app.stockmaster.data.remote.ErpApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: com.app.stockmaster.data.local.dao.ItemDao,
    private val erpApi: com.app.stockmaster.data.remote.ErpApi,
    private val bridgeApi: com.app.stockmaster.data.remote.BridgeApi
) {
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()
    
    fun getLowStockItems(): Flow<List<ItemEntity>> = itemDao.getLowStockItems()
    
    fun getTotalItemCount(): Flow<Int> = itemDao.getTotalItemCount()
    
    fun searchItems(query: String): Flow<List<ItemEntity>> = itemDao.searchItems(query)
    
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)
    
    suspend fun updateItem(item: ItemEntity) = itemDao.updateItem(item)
    
    suspend fun deleteItem(id: Int) = itemDao.deleteItem(id)

    suspend fun syncWithBridge(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = bridgeApi.getProducts()
            if (response.isSuccessful) {
                val bridgeResponse = response.body()
                if (bridgeResponse?.success == true) {
                    bridgeResponse.products?.forEach { bp ->
                        val existing = itemDao.getItemByTinyId(bp.id.toString()) 
                            ?: itemDao.getItemBySku(bp.id.toString()) // Fallback to SKU if bridge ID is alphanumeric
                        
                        val entity = ItemEntity(
                            id = existing?.id ?: 0,
                            name = bp.name,
                            sku = bp.id.toString(),
                            barcode = "", 
                            category = bp.category ?: "Geral",
                            costPrice = 0.0,
                            salePrice = try { bp.price?.replace("R$", "")?.replace(",", ".")?.toDouble() ?: 0.0 } catch (e: Exception) { 0.0 },
                            currentStock = bp.quantity,
                            minStockAlert = bp.minimum_stock,
                            tinyId = bp.id.toString(), // We'll keep using tinyId field for the remote ID
                            imageUri = bp.image,
                            updatedAt = System.currentTimeMillis()
                        )
                        itemDao.insertItem(entity)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(bridgeResponse?.error ?: "Erro desconhecido na Bridge"))
                }
            } else {
                Result.failure(Exception("Erro na Bridge: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRemoteStock(itemId: Int, newQuantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = itemDao.getItemById(itemId) ?: return@withContext Result.failure(Exception("Item não encontrado"))
            val remoteId = item.tinyId ?: return@withContext Result.failure(Exception("Item não possui ID remoto"))
            
            val response = bridgeApi.updateQuantity(remoteId, mapOf("quantity" to newQuantity))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Erro ao atualizar estoque na Bridge"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRemoteProduct(item: ItemEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bp = BridgeProduct(
                id = item.sku, // Bridge will use SKU as ID if not specified, or it will generate one
                name = item.name,
                quantity = item.currentStock,
                minimum_stock = item.minStockAlert,
                category = item.category,
                price = item.salePrice.toString(),
                brand = null,
                color = null,
                image = item.imageUri
            )
            val response = bridgeApi.addProduct(bp)
            if (response.isSuccessful && response.body()?.success == true) {
                // Update local item with the remote ID if returned
                val remoteProduct = response.body()?.product
                if (remoteProduct != null) {
                    itemDao.insertItem(item.copy(tinyId = remoteProduct.id.toString()))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Erro ao adicionar produto na Bridge"))
            }
        } catch (e: Exception) {
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
                                barcode = "", 
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
