package com.example.marketplace.data.entity

import org.junit.Assert.*
import org.junit.Test

class ProdutoCompraTest {
    private val produto = ProdutoEntity(emailNegociante = "vendedor@teste.com", nomeNegociante = "Ana",
        nome = "Livro", preco = "10", descricao = "Livro usado")

    @Test fun vendedorNaoPodeComprarSeuProduto() {
        assertFalse(produto.podeSerCompradoPor("vendedor@teste.com"))
    }

    @Test fun outroNegociantePodeComprar() {
        assertTrue(produto.podeSerCompradoPor("cliente@teste.com"))
    }

    @Test fun usuarioSemIdentificacaoNaoPodeComprar() {
        assertFalse(produto.podeSerCompradoPor(""))
    }
}
