package com.app.stockmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val type: String,           // "INBOUND" | "OUTBOUND" | "ADJUSTMENT"
    val quantity: Int,
    val reason: String,
    val notes: String? = null,
    val date: Long = System.currentTimeMillis()
)
