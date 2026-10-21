package com.example.marketplace

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.marketplace.ui.mapa.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RotaPersistenciaTest {
    @Test fun rotaAgrupadaPreservaTodosOsPedidosAposSerializacao() {
        val coleta = Coordenadas(-21.7815354, -48.2096242)
        val entrega = Coordenadas(-21.8068607, -48.129343)
        val rota = RotaGerada(coleta, listOf(
            ParadaRota(1, "Coleta", coleta, TipoParada.COLETA, listOf(1, 2)),
            ParadaRota(1, "Entrega", entrega, TipoParada.ENTREGA, listOf(1, 2))), listOf(coleta, entrega))
        val salva = rota.paraEntity("motorista")
        assertEquals(listOf(1, 2), salva.pedidoIds())
        assertEquals(rota, salva.paraRotaGerada())
    }

    @Test fun rotaAntigaAgrupaApenasVisitasConsecutivasSemAlterarTrajeto() {
        val coleta = Coordenadas(-21.7815354, -48.2096242)
        val entrega = Coordenadas(-21.8068607, -48.129343)
        val rota = RotaGerada(coleta, listOf(
            ParadaRota(1, "Coleta", coleta, TipoParada.COLETA),
            ParadaRota(2, "Coleta", coleta, TipoParada.COLETA),
            ParadaRota(1, "Entrega", entrega), ParadaRota(2, "Entrega", entrega)), listOf(coleta, entrega))
        val salva = rota.paraEntity("motorista")
        val destinos = JSONArray(salva.destinosJson)
        for (i in 0 until destinos.length()) destinos.getJSONObject(i).remove("pedidoIds")
        val restaurada = salva.copy(destinosJson = destinos.toString()).paraRotaGerada()
        assertEquals(2, restaurada.paradas.size)
        assertEquals(listOf(1, 2), restaurada.paradas[0].pedidoIds)
        assertEquals(listOf(1, 2), restaurada.paradas[1].pedidoIds)
        assertEquals(rota.geometria, restaurada.geometria)
    }
}
