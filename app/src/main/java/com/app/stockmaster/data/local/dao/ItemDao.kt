package com.app.stockmaster.data.local.dao

import androidx.room.*
import com.app.stockmaster.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE currentStock <= minStockAlert AND isActive = 1")
    fun getLowStockItems(): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items WHERE isActive = 1")
    fun getTotalItemCount(): Flow<Int>

    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET currentStock = currentStock + :qty, updatedAt = :now WHERE id = :id")
    suspend fun addStock(id: Int, qty: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE items SET currentStock = currentStock - :qty, updatedAt = :now WHERE id = :id")
    suspend fun removeStock(id: Int, qty: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Int)

    @Query("SELECT * FROM items WHERE tinyId = :tinyId LIMIT 1")
    suspend fun getItemByTinyId(tinyId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE sku = :sku LIMIT 1")
    suspend fun getItemBySku(sku: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): ItemEntity?
}
