package com.app.stockmaster.data.local.dao

import androidx.room.*
import com.app.stockmaster.data.local.entity.TransactionEntity
import com.app.stockmaster.data.local.model.DayActivity
import com.app.stockmaster.data.local.model.TransactionWithItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.*, i.name as itemName 
        FROM transactions t 
        JOIN items i ON t.itemId = i.id 
        ORDER BY t.date DESC LIMIT 50
    """)
    fun getRecentTransactions(): Flow<List<TransactionWithItem>>

    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :startOfDay")
    fun getTodayMovements(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT strftime('%w', date/1000, 'unixepoch') as day, SUM(quantity) as total
        FROM transactions
        WHERE date >= :weekStart
        GROUP BY day
    """)
    fun getWeeklyActivity(weekStart: Long): Flow<List<DayActivity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)
}
