package com.example.marketplace.ui.screens.entregador

import com.example.marketplace.ui.mapa.ParadaRota
import com.example.marketplace.ui.mapa.TipoParada

data class ProgressoParada(val concluidos: Int, val concluida: Boolean, val podeAlterar: Boolean)

fun progressoParada(parada: ParadaRota, pedidos: Map<Int, PedidoTemp>): ProgressoParada {
    val lista = parada.pedidoIds.mapNotNull { pedidos[it] }
    val carregados = lista.size == parada.pedidoIds.size && lista.isNotEmpty()
    val coleta = parada.tipo == TipoParada.COLETA
    val concluidos = lista.count { if (coleta) it.coletado else it.statusEntrega == "ENTREGUE" }
    return ProgressoParada(concluidos, carregados && concluidos == lista.size,
        carregados && lista.all { it.statusEntrega in listOf("EM_ROTA", "ENTREGUE") } &&
            if (coleta) lista.none { it.statusEntrega == "ENTREGUE" } else lista.all { it.coletado })
}
