package com.app.stockmaster.data.local.model

data class DayActivity(
    val day: String,
    val total: Int
)

data class TransactionWithItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val type: String,
    val quantity: Int,
    val reason: String,
    val notes: String? = null,
    val date: Long
)
