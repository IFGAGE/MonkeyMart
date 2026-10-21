package com.example.marketplace.data.sync

import com.example.marketplace.data.entity.PedidoEntity
import org.junit.Assert.*
import org.junit.Test

class PedidoRemotoTest {
    @Test fun snapshotAtrasadoNaoDesfazReservaSincronizada() {
        val local = pedido(id = 7, status = "EM_ROTA").copy(revisao = 2, emailEntregador = "motorista")
        assertEquals(local, mesclarPedido(local, pedido(status = "PRONTO_RETIRADA").copy(revisao = 1)))
    }
    @Test fun transportaVendedorColetaEntregaERevisao() {
        val original = pedido().copy(emailNegociante = "vendedor", enderecoColeta = "Rua C, 1",
            coletaLatitude = -16.7, coletaLongitude = -49.3, entregaLatitude = -16.8,
            entregaLongitude = -49.4, emailEntregador = "motorista", revisao = 2, coletado = true)
        assertEquals(original, pedidoRemoto(original.firestoreId, dadosPedido(original)))
    }
    @Test fun pedidoLegadoEntregueJaTemColetaConcluida() {
        val dados = dadosPedido(pedido(status = "ENTREGUE")).toMutableMap()
        dados.remove("coletado")
        assertTrue(pedidoRemoto("legado", dados)!!.coletado)
        dados["statusEntrega"] = "EM_ROTA"
        assertFalse(pedidoRemoto("legado", dados)!!.coletado)
    }
    @Test fun revisaoRemotaMaisNovaSuperaAlteracaoLocalObsoleta() {
        val local = pedido(id = 7, synced = false, status = "PRONTO_RETIRADA").copy(revisao = 1)
        val remoto = pedido(status = "EM_ROTA").copy(revisao = 2, emailEntregador = "motorista")
        val resultado = mesclarPedido(local, remoto)
        assertEquals("EM_ROTA", resultado.statusEntrega)
        assertEquals(7, resultado.id)
    }
    private fun pedido(id: Int = 0, synced: Boolean = true, status: String = "PENDENTE") = PedidoEntity(
        id, "cliente@teste.invalid", "Rua A, 10", "1x Produto", "10.00", status, synced, "remoto-a"
    )
    @Test fun recebeDocumentoLegadoSemUsarIdRemotoComoIdLocal() {
        val dados = dadosPedido(pedido()).toMutableMap()
        dados["id"] = 999
        dados.remove("firestoreId")
        val recebido = pedidoRemoto("999", dados)!!
        assertEquals(0, recebido.id)
        assertEquals("999", recebido.firestoreId)
        assertTrue(recebido.isSynced)
    }
    @Test fun doisDispositivosNaoCompartilhamIdDeNovosDocumentos() {
        val a = PedidoEntity(emailCliente="a", enderecoEntrega="A", resumoItens="A", valorTotal="1")
        val b = PedidoEntity(emailCliente="b", enderecoEntrega="B", resumoItens="B", valorTotal="2")
        assertNotEquals(a.firestoreId, b.firestoreId)
    }
    @Test fun documentoIncompletoNaoEntraNoBanco() {
        assertNull(pedidoRemoto("a", mapOf("emailCliente" to "a")))
        assertNull(pedidoRemoto("", dadosPedido(pedido())))
    }
    @Test fun atualizaStatusMantendoIdLocal() {
        val local = pedido(id=7)
        val remoto = pedido(status="ENTREGUE")
        val resultado = mesclarPedido(local, remoto)
        assertEquals(7, resultado.id)
        assertEquals("ENTREGUE", resultado.statusEntrega)
    }
    @Test fun preservaAlteracaoOfflinePendente() {
        val local = pedido(id=7, synced=false, status="ENTREGUE")
        assertEquals(local, mesclarPedido(local, pedido()))
    }
    @Test fun pedidoNovoRecebeIdLocalAutomatico() {
        assertEquals(0, mesclarPedido(null, pedido(id=999)).id)
    }
    @Test fun dadosDaNuvemNaoLevamFlagsLocais() {
        val dados = dadosPedido(pedido())
        assertFalse(dados.containsKey("id"))
        assertFalse(dados.containsKey("isSynced"))
        assertEquals("Rua A, 10", dados["enderecoEntrega"])
    }
}
