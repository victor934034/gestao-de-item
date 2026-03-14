package com.app.stockmaster.data.remote

import retrofit2.Response
import retrofit2.http.GET
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
}
