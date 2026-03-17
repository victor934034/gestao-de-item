package com.app.stockmaster.data.remote

data class BridgeProductResponse(
    val success: Boolean,
    val products: List<BridgeProduct>? = null,
    val error: String? = null
)

data class BridgeProduct(
    val id: Any, // Received as number/string from Supabase
    val nome: String,
    val quantidade: Double,
    val modo_estocagem: String? = null,
    val custo: Double? = null,
    val preco_venda: Double? = null,
    val foto_url: String? = null,
    val barcode: String? = null
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

data class BridgeVersionResponse(
    val success: Boolean,
    val version: BridgeVersion? = null,
    val error: String? = null
)

data class BridgeVersion(
    val latestVersion: String,
    val minVersion: String,
    val apkUrl: String
)
