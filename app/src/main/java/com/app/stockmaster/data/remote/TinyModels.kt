package com.app.stockmaster.data.remote

data class TinyProductSearchResponse(
    val retorno: TinyProductSearchReturn
)

data class TinyProductSearchReturn(
    val status: String,
    val pagina: Int? = null,
    val numero_paginas: Int? = null,
    val produtos: List<TinyProductWrapped>? = null,
    val erros: List<TinyError>? = null
)

data class TinyError(
    val erro: String
)

data class TinyProductWrapped(
    val produto: TinyProduct
)

data class TinyProduct(
    val id: String,
    val codigo: String?,
    val nome: String,
    val preco: Double?,
    val preco_promocional: Double?,
    val unidade: String?
)

data class TinyStockResponse(
    val retorno: TinyStockReturn
)

data class TinyStockReturn(
    val status: String,
    val produto: TinyStockProduct?,
    val erros: List<TinyError>? = null
)

data class TinyStockProduct(
    val id: String,
    val codigo: String?,
    val nome: String,
    val estoque_atual: Double?,
    val saldo: Double?
)

// Keeping the quote models for reference if the user needs them later
data class QuoteRequest(val cepDestino: String)
data class QuoteResponse(val retorno: QuoteReturnInfo)
data class QuoteReturnInfo(
    val status: String,
    val cotacoes: List<Quote>? = null
)
data class Quote(val nome: String, val valor: Double, val prazoEnvio: Int)
