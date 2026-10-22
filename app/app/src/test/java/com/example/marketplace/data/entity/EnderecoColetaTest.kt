package com.example.marketplace.data.entity

import org.junit.Assert.*
import org.junit.Test

class EnderecoColetaTest {
    private val endereco = EnderecoColeta(endereco = "Rua Um, 10, Goiânia, GO", latitude = -16.68, longitude = -49.25)

    @Test fun pontoConfirmadoPermiteCadastroComCampoUnico() {
        assertTrue(endereco.valido())
    }

    @Test fun textoSemConfirmacaoNoMapaNaoPermiteCadastro() {
        assertFalse(endereco.copy(latitude = null).valido())
        assertFalse(endereco.copy(endereco = " ").valido())
    }

    @Test fun enderecoLegadoContinuaDisponivelParaRevalidacao() {
        val antigo = EnderecoColeta("74000000", "Rua Um", "10", "", "Centro", "Goiânia", "GO")
        assertEquals("Rua Um, 10, Centro, Goiânia, GO, 74000000", antigo.texto())
        assertFalse(antigo.valido())
    }
}
