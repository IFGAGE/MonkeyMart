package com.example.marketplace.ui.acompanhamento

import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.proximidade.pontoAoNorte
import org.junit.Assert.*
import org.junit.Test

class PosicaoEntregaTest {
    private val agora = 1_800_000_000_000L
    private val destino = Coordenadas(-21.784, -48.2107)
    private fun posicao(metros: Double): PosicaoEntrega {
        val ponto = pontoAoNorte(destino, metros)
        return PosicaoEntrega(ponto.latitude, ponto.longitude, 10f, agora, "entregador@teste.invalid", destino.latitude, destino.longitude)
    }

    @Test fun distanciaDiminuiSemMudarOEstadoDoPedido() {
        val distancias = listOf(400.0, 300.0, 250.0, 150.0, 100.0).map { posicao(it).distanciaMetros() }
        assertTrue(distancias.zipWithNext().all { (antes, depois) -> depois < antes })
        assertEquals(400.0, distancias.first(), 0.1)
        assertEquals(100.0, distancias.last(), 0.1)
        assertFalse(podeRegistrarAproximacao(posicao(250.0), agora))
        assertTrue(podeRegistrarAproximacao(posicao(150.0), agora))
    }

    @Test fun naoAvisaComPosicaoAntigaFuturaImprecisaOuInvalida() {
        assertFalse(podeRegistrarAproximacao(posicao(100.0).copy(capturadaEm = agora - 30_001), agora))
        assertFalse(podeRegistrarAproximacao(posicao(100.0).copy(capturadaEm = agora + 1), agora))
        assertFalse(podeRegistrarAproximacao(posicao(100.0).copy(precisao = 101f), agora))
        assertFalse(podeRegistrarAproximacao(posicao(100.0).copy(latitude = Double.NaN), agora))
    }

    @Test fun rejeitaRastreamentoDeOutroEntregadorDestinoOuPedidoEncerrado() {
        val pedido = PedidoEntity(emailCliente = "cliente@teste.invalid", enderecoEntrega = "Rua A", resumoItens = "Produto", valorTotal = "10",
            coletado = true, statusEntrega = "EM_ROTA", emailEntregador = "entregador@teste.invalid",
            entregaLatitude = destino.latitude, entregaLongitude = destino.longitude)
        val ponto = posicao(100.0)
        assertTrue(ponto.corresponde(pedido))
        assertFalse(ponto.corresponde(pedido.copy(emailEntregador = "outro@teste.invalid")))
        assertFalse(ponto.corresponde(pedido.copy(entregaLatitude = -22.0)))
        assertFalse(ponto.corresponde(pedido.copy(statusEntrega = "ENTREGUE")))
        assertTrue(ponto.corresponde(pedido.copy(coletado = false)))
    }

    @Test fun leOsDadosRecebidosSemAceitarCamposAusentesOuCoordenadasInvalidas() {
        val original = posicao(100.0)
        assertEquals(original, lerPosicao(original.dados()))
        assertNull(lerPosicao(null))
        assertNull(lerPosicao(original.dados() - "capturadaEm"))
        assertNull(lerPosicao(original.dados() + ("longitude" to 181.0)))
        assertNull(lerPosicao(original.dados() + ("precisao" to Float.NaN)))
    }
}
