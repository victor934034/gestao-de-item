package com.app.stockmaster.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ErpApi {
    @GET("produtos.pesquisa.php")
    suspend fun searchProducts(
        @Query("pesquisa") query: String? = null,
        @Query("pagina") page: Int = 1
    ): Response<TinyProductSearchResponse>

    @GET("produto.obter.estoque.php")
    suspend fun getProductStock(
        @Query("id") id: String
    ): Response<TinyStockResponse>

    @FormUrlEncoded
    @POST("produto.incluir.php")
    suspend fun createProduct(
        @Field("produto") productJson: String
    ): Response<TinyCreateUpdateResponse>

    @FormUrlEncoded
    @POST("produto.alterar.php")
    suspend fun updateProduct(
        @Field("produto") productJson: String
    ): Response<TinyCreateUpdateResponse>
}
