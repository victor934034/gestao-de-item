package com.app.stockmaster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.stockmaster.data.local.dao.CategoryDao
import com.app.stockmaster.data.local.dao.ItemDao
import com.app.stockmaster.data.local.dao.TransactionDao
import com.app.stockmaster.data.local.entity.CategoryEntity
import com.app.stockmaster.data.local.entity.ItemEntity
import com.app.stockmaster.data.local.entity.TransactionEntity

@Database(
    entities = [ItemEntity::class, TransactionEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "stockmaster_db"
    }
}
