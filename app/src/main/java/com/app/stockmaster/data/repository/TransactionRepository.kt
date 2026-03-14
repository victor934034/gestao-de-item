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
    private val itemRepository: ItemRepository
) {
    fun getRecentTransactions(): Flow<List<TransactionWithItem>> = transactionDao.getRecentTransactions()
    
    fun getTodayMovements(startOfDay: Long): Flow<Int> = transactionDao.getTodayMovements(startOfDay)
    
    fun getWeeklyActivity(weekStart: Long): Flow<List<DayActivity>> = transactionDao.getWeeklyActivity(weekStart)
    
    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        val delta = if (transaction.type == "INBOUND") transaction.quantity else -transaction.quantity
        
        // Use repository to update local and trigger remote sync
        val item = itemRepository.getAllItems().first().find { it.id == transaction.itemId } ?: return
        val newStock = item.currentStock + delta
        
        itemRepository.updateItem(item.copy(currentStock = newStock, updatedAt = System.currentTimeMillis()))
        
        // Trigger remote sync in the background or wait
        itemRepository.updateRemoteStock(transaction.itemId, newStock)
    }
}
