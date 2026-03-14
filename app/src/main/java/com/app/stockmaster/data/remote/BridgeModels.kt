package com.app.stockmaster.data.remote

data class BridgeProductResponse(
    val success: Boolean,
    val products: List<BridgeProduct>? = null,
    val error: String? = null
)

data class BridgeProduct(
    val id: Any, // Can be String or Int depending on Supabase config
    val nome: String,
    val quantidade: Double,
    val modo_estocagem: String? = null,
    val custo: Double? = null,
    val preco_venda: Double? = null,
    val foto_url: String? = null
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
