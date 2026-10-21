package com.example.rotaspoc

import org.junit.Assert.*
import org.junit.Test

class OrdenacaoTest {
    @Test fun encontraOrdemMelhorQueAVizinhancaImediata() {
        // Ir primeiro ao ponto A (custo 1) termina com custo 101;
        // passar por B e depois A custa somente 3.
        val matriz = listOf(listOf<Double?>(0.0, 1.0, 2.0),
            listOf<Double?>(1.0, 0.0, 100.0), listOf<Double?>(2.0, 1.0, 0.0))
        assertEquals(listOf(1, 0), ordenarParadas(matriz))
    }

    @Test fun campoEditadoOuInvalidoNaoEntraNaRota() {
        val ponto = Ponto("Rua A", Coordenadas(-21.78, -48.20))
        val campos = listOf(CampoEndereco(1, "Rua A", ponto = ponto),
            CampoEndereco(2, "Erro", erro = "Não encontrado"), CampoEndereco(3),
            CampoEndereco(4, "Outra rua", buscando = true))
        assertEquals(listOf(ponto), pontosValidos(campos))
        assertTrue(pontosValidos(listOf(campos[0].comTexto("Rua B"))).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun naoCriaCaminhoOndeNaoExisteTrecho() {
        ordenarParadas(listOf(listOf(0.0, null, null), listOf(null, 0.0, null), listOf(null, null, 0.0)))
    }
}
