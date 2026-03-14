package com.app.stockmaster.data.remote

data class BridgeProductResponse(
    val success: Boolean,
    val products: List<BridgeProduct>? = null,
    val error: String? = null
)

data class BridgeProduct(
    val id: Any, // Can be String or Int depending on Supabase config
    val name: String,
    val quantity: Int,
    val minimum_stock: Int,
    val category: String?,
    val price: String?,
    val brand: String?,
    val color: String?,
    val image: String?
)

data class BridgeUpdateResponse(
    val success: Boolean,
    val product: BridgeProduct? = null,
    val error: String? = null
)

data class BridgeStatusResponse(
    val initialized: Boolean,
    val table_accessible: Boolean,
    val count: Int? = null,
    val error: String? = null
)
