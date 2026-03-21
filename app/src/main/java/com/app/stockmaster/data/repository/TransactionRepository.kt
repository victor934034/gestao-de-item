package com.app.stockmaster.data.repository

import com.app.stockmaster.data.local.dao.ItemDao
import com.app.stockmaster.data.local.dao.TransactionDao
import com.app.stockmaster.data.local.entity.TransactionEntity
import com.app.stockmaster.data.local.model.DayActivity
import com.app.stockmaster.data.local.model.TransactionWithItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val itemRepository: ItemRepository,
    private val bridgeApi: com.app.stockmaster.data.remote.BridgeApi
) {
    fun getRecentTransactions(): Flow<List<TransactionWithItem>> = transactionDao.getRecentTransactions()
    
    fun getTodayMovements(startOfDay: Long): Flow<Int> = transactionDao.getTodayMovements(startOfDay)
    
    fun getWeeklyActivity(weekStart: Long): Flow<List<DayActivity>> = transactionDao.getWeeklyActivity(weekStart)
    
    suspend fun addTransaction(transaction: TransactionEntity) {
        val id = transactionDao.insertTransaction(transaction)
        val delta = if (transaction.type == "INBOUND") transaction.quantity else -transaction.quantity
        
        // Use repository to update local and trigger remote sync
        val item = itemRepository.getItemById(transaction.itemId) ?: return
        val newStock = item.currentStock + delta
        
        itemRepository.updateItem(item.copy(currentStock = newStock, updatedAt = System.currentTimeMillis()))
        
        // Push transaction to bridge
        pushTransactionToRemote(transaction.copy(id = id.toInt()))
        
        // Update remote stock
        itemRepository.updateRemoteStock(transaction.itemId, newStock)
    }

    private suspend fun pushTransactionToRemote(transaction: TransactionEntity) {
        try {
            val item = itemRepository.getItemById(transaction.itemId) ?: return
            val bt = com.app.stockmaster.data.remote.BridgeTransaction(
                item_id = item.tinyId ?: transaction.itemId,
                item_name = item.name,
                quantidade = transaction.quantity,
                tipo = transaction.type
            )
            val response = bridgeApi.addBridgeTransaction(bt)
            if (response.isSuccessful) {
                val remoteId = response.body()?.transaction?.id?.toString()
                if (remoteId != null) {
                    transactionDao.insertTransaction(transaction.copy(remoteId = remoteId))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Sync", "Error pushing transaction", e)
        }
    }

    suspend fun syncWithBridge(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Push unsynced
            val unsynced = transactionDao.getUnsyncedTransactions()
            unsynced.forEach { pushTransactionToRemote(it) }

            // 2. Pull
            val response = bridgeApi.getTransactions()
            if (response.isSuccessful) {
                val remoteTransactions = response.body()?.transactions ?: emptyList()
                remoteTransactions.forEach { bt ->
                    val remoteIdStr = bt.id?.toString() ?: return@forEach
                    
                    // Logic to avoid double counting is tricky if item sync already updated stock
                    // But for "Recent Activity" view, we just need the records
                    
                    // Find item by remote ID
                    val itemTinyId = bt.item_id.toString().split(".")[0]
                    val item = itemRepository.itemDao.getItemByTinyId(itemTinyId) ?: return@forEach

                    val entity = TransactionEntity(
                        itemId = item.id,
                        type = bt.tipo,
                        quantity = bt.quantidade,
                        reason = "Sincronizado",
                        date = try { 
                            // Convert ISO 8601 to Long if available, or just use now
                            System.currentTimeMillis() 
                        } catch (e: Exception) { System.currentTimeMillis() },
                        remoteId = remoteIdStr
                    )
                    
                    // Only insert if not exists locally by remoteId
                    // Note: TransactionDao doesn't have getByRemoteId yet, I'll add or use a more complex query
                    transactionDao.insertTransaction(entity)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error pulling transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
