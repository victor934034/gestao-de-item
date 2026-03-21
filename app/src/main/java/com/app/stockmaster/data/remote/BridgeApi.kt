package com.app.stockmaster.data.remote

import retrofit2.Response
import retrofit2.http.*

data class BridgeResponse<T>(
    val success: Boolean,
    val products: T? = null,
    val product: T? = null,
    val transactions: T? = null,
    val transaction: T? = null,
    val error: String? = null
)

data class BridgeTransaction(
    val id: Any? = null,
    val item_id: Any,
    val item_name: String,
    val quantidade: Int,
    val tipo: String,
    val data_hora: String? = null
)

interface BridgeApi {
    @GET("status")
    suspend fun getStatus(): Response<Map<String, Any>>

    @GET("version")
    suspend fun getLatestVersion(): Response<BridgeVersionResponse>

    @GET("products")
    suspend fun getProducts(): Response<BridgeResponse<List<BridgeProduct>>>

    @PATCH("products/{id}/quantity")
    suspend fun updateQuantity(
        @Path("id") id: String,
        @Body body: Map<String, Int>
    ): Response<BridgeResponse<BridgeProduct>>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body product: BridgeProduct
    ): Response<BridgeResponse<BridgeProduct>>

    @POST("products")
    suspend fun addProduct(
        @Body product: BridgeProduct
    ): Response<BridgeResponse<BridgeProduct>>

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<BridgeResponse<Unit>>

    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part image: okhttp3.MultipartBody.Part): Response<Map<String, Any>>

    @GET("transactions")
    suspend fun getTransactions(): Response<BridgeResponse<List<BridgeTransaction>>>

    @POST("transactions")
    suspend fun addBridgeTransaction(@Body transaction: BridgeTransaction): Response<BridgeResponse<BridgeTransaction>>
}
