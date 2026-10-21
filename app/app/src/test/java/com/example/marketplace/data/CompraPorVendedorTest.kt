package com.example.marketplace.data

import com.example.marketplace.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class CompraPorVendedorTest {
    private val entrega = EnderecoValidado("Rua de entrega, 10, Goiânia", -16.68, -49.25)
    private fun produto(vendedor: String, preco: String) = ProdutoEntity(emailNegociante = vendedor,
        nomeNegociante = vendedor, nome = "Livro", descricao = "", preco = preco,
        enderecoColeta = "Coleta $vendedor", coletaLatitude = -16.7, coletaLongitude = -49.3)

    @Test fun separaVendedoresESomaSomenteSeusItens() {
        val pedidos = criarPedidos("cliente", listOf(ItemCompra(produto("ana", "10,50"), 2),
            ItemCompra(produto("edu", "20.00"), 1), ItemCompra(produto("ana", "5"), 1)), entrega)
        assertEquals(2, pedidos.size)
        assertEquals("26.00", pedidos.single { it.emailNegociante == "ana" }.valorTotal)
        assertEquals("20.00", pedidos.single { it.emailNegociante == "edu" }.valorTotal)
        assertEquals("Coleta ana", pedidos[0].enderecoColeta)
        assertEquals(-16.68, pedidos[0].entregaLatitude!!, 0.0)
        assertEquals("AGUARDANDO_PREPARO", pedidos[0].statusEntrega)
        assertNotEquals(pedidos[0].firestoreId, pedidos[1].firestoreId)
    }

    @Test(expected = IllegalArgumentException::class) fun rejeitaCompraPropria() {
        criarPedidos("ana", listOf(ItemCompra(produto("ana", "10"), 1)), entrega)
    }
    @Test(expected = IllegalArgumentException::class) fun rejeitaColetaNaoValidada() {
        criarPedidos("cliente", listOf(ItemCompra(produto("ana", "10").copy(coletaLatitude = null), 1)), entrega)
    }
    @Test(expected = IllegalArgumentException::class) fun rejeitaQuantidadeInvalida() {
        criarPedidos("cliente", listOf(ItemCompra(produto("ana", "10"), 0)), entrega)
    }
    @Test fun enderecoExigeCoordenadasValidas() {
        assertFalse(entrega.copy(latitude = Double.NaN).valido())
        assertFalse(entrega.copy(longitude = 181.0).valido())
        assertFalse(entrega.copy(endereco = " ").valido())
    }
}
