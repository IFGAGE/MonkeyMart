package com.example.marketplace.ui.screens.entregador

data class PedidoTemp(
    val id: Int,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String,
    val enderecoColeta: String = "",
    val coletaLatitude: Double? = null,
    val coletaLongitude: Double? = null,
    val entregaLatitude: Double? = null,
    val entregaLongitude: Double? = null,
    val coletado: Boolean = false
)
