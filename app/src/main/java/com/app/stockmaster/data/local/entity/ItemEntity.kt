package com.app.stockmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String,
    val barcode: String,
    val category: String,
    val imageUri: String? = null,
    val costPrice: Double,
    val salePrice: Double,
    val currentStock: Int,
    val minStockAlert: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tinyId: String? = null
)
