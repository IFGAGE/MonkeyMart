package com.example.marketplace.data.sync

import com.example.marketplace.data.entity.ProdutoEntity
import org.junit.Assert.*
import org.junit.Test

class ProdutoRemotoTest {
    private val atual = ProdutoEntity(id = 7, emailNegociante = "ana", nomeNegociante = "Ana", nome = "Livro",
        preco = "10", descricao = "", enderecoColeta = "Coleta nova", coletaLatitude = -16.7,
        coletaLongitude = -49.3, fotoPathLocal = "/foto-local.jpg", isSynced = true, revisao = 2)

    @Test fun snapshotAtrasadoNaoReverteColetaConfirmada() {
        assertEquals(atual, mesclarProduto(atual, atual.copy(id = 99, revisao = 1, enderecoColeta = "Coleta velha")))
    }
    @Test fun recebimentoPreservaIdEFotoLocais() {
        val resultado = mesclarProduto(atual, atual.copy(id = 99, fotoPathLocal = null,
            revisao = 3, enderecoColeta = "Coleta mais recente"))
        assertEquals(7, resultado.id)
        assertEquals("/foto-local.jpg", resultado.fotoPathLocal)
        assertEquals("Coleta mais recente", resultado.enderecoColeta)
    }
    @Test fun produtoDeOutroDispositivoNaoReutilizaSeuCaminhoDeFoto() {
        val resultado = produtoRemoto("documento", mapOf("emailNegociante" to "ana", "nome" to "Livro",
            "fotoPathLocal" to "/foto-outro-aparelho.jpg", "revisao" to 2L))!!
        assertEquals("documento", resultado.firestoreId)
        assertNull(resultado.fotoPathLocal)
        assertEquals(2, resultado.revisao)
    }
}
