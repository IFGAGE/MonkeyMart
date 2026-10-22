package com.example.marketplace.ui.mapa

import kotlin.math.roundToLong

private data class LocalParada(val tipo: TipoParada, val endereco: String, val latitude: Long, val longitude: Long)

private fun ParadaRota.local() = LocalParada(tipo,
    endereco.trim().replace(Regex("\\s+"), " ").lowercase(),
    (coordenadas.latitude * 1_000_000).roundToLong(), (coordenadas.longitude * 1_000_000).roundToLong())

// Mesmo ponto e tipo, mantendo separados endereços com complementos diferentes.
fun agruparParadas(paradas: List<ParadaRota>): List<ParadaRota> = paradas.groupBy { it.local() }.values.map { grupo ->
    grupo.first().copy(pedidoIds = grupo.flatMap { it.pedidoIds }.distinct())
}

// Rotas já salvas conservam a sequência e a geometria; só une visitas consecutivas.
fun agruparParadasConsecutivas(paradas: List<ParadaRota>): List<ParadaRota> {
    val resultado = mutableListOf<ParadaRota>()
    for (parada in paradas) {
        val ultima = resultado.lastOrNull()
        if (ultima != null && ultima.local() == parada.local()) {
            resultado[resultado.lastIndex] = ultima.copy(pedidoIds = (ultima.pedidoIds + parada.pedidoIds).distinct())
        } else resultado += parada
    }
    return resultado
}
