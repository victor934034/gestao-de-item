package com.app.stockmaster.data.remote

import retrofit2.Response
import retrofit2.http.*

interface BridgeApi {
    @GET("api/stock/products")
    suspend fun getProducts(): Response<BridgeProductResponse>

    @GET("api/stock/status")
    suspend fun getStatus(): Response<BridgeStatusResponse>

    @PATCH("api/stock/products/{id}/quantity")
    suspend fun updateQuantity(
        @Path("id") id: String,
        @Body body: Map<String, Int>
    ): Response<BridgeUpdateResponse>

    @PUT("api/stock/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body product: BridgeProduct
    ): Response<BridgeUpdateResponse>

    @POST("api/stock/products")
    suspend fun addProduct(
        @Body product: BridgeProduct
    ): Response<BridgeUpdateResponse>

    @DELETE("api/stock/products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<BridgeProductResponse>
}
