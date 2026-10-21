package com.example.marketplace.ui.mapa

import org.junit.Assert.*
import org.junit.Test

class OrdenacaoParadasTest {
    @Test fun paradaAgrupadaMantemTodosOsPedidos() {
        val ponto = Coordenadas(-21.7815354, -48.2096242)
        val paradas = agruparParadas(listOf(ParadaRota(1, "Rua A, 500", ponto, TipoParada.COLETA),
            ParadaRota(2, "Rua A, 500", ponto, TipoParada.COLETA)))
        assertEquals(1, paradas.size)
        assertEquals(listOf(1, 2), paradas.single().pedidoIds)
    }
    @Test fun entregaMaisProximaNaoPodeAntecederSuaColeta() {
        val p = Coordenadas(-16.0, -49.0)
        val paradas = listOf(ParadaRota(1, "coleta", p, TipoParada.COLETA), ParadaRota(1, "entrega", p, TipoParada.ENTREGA))
        val ordem = ordenarParadasComColeta(listOf(listOf(0.0, 10.0, 1.0),
            listOf(10.0, 0.0, 2.0), listOf(1.0, 2.0, 0.0)), paradas)
        assertEquals(listOf(0, 1), ordem)
    }
    @Test fun respeitaColetaDeCadaPedidoComVariosVendedores() {
        val p = Coordenadas(-16.0, -49.0)
        val paradas = listOf(ParadaRota(1, "C1", p, TipoParada.COLETA), ParadaRota(1, "E1", p),
            ParadaRota(2, "C2", p, TipoParada.COLETA), ParadaRota(2, "E2", p))
        val matriz = List(5) { List<Double?>(5) { 1.0 } }
        val ordem = ordenarParadasComColeta(matriz, paradas)
        assertEquals(4, ordem.toSet().size)
        assertTrue(ordem.indexOf(0) < ordem.indexOf(1))
        assertTrue(ordem.indexOf(2) < ordem.indexOf(3))
    }
    @Test(expected = IllegalArgumentException::class) fun rejeitaTrechoSemRota() {
        ordenarParadasComColeta(listOf(listOf(0.0, null), listOf(null, 0.0)),
            listOf(ParadaRota(1, "C", Coordenadas(0.0, 0.0), TipoParada.COLETA)))
    }
}
