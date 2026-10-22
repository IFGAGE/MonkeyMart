package com.example.marketplace.ui.mapa

data class Coordenadas(val latitude: Double, val longitude: Double)

data class ParadaRota(
    val pedidoId: Int,
    val endereco: String,
    val coordenadas: Coordenadas,
    val tipo: TipoParada = TipoParada.ENTREGA,
    val pedidoIds: List<Int> = listOf(pedidoId)
)

data class RotaGerada(
    val origem: Coordenadas,
    val paradas: List<ParadaRota>,
    val geometria: List<Coordenadas>
)
