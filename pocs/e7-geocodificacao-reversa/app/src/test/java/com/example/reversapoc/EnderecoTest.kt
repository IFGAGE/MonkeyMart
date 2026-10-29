package com.example.reversapoc

import org.junit.Assert.*
import org.junit.Test

class EnderecoTest {
    @Test fun formataEnderecoCompleto() {
        val partes = PartesEndereco("Avenida Paulista", "1578", "Bela Vista", "São Paulo", "SP", "01310-200", "BR")
        assertEquals("Avenida Paulista, 1578 - Bela Vista, São Paulo - SP, 01310-200", formatarEndereco(partes))
    }
    @Test fun camposAusentesSaoOmitidos() {
        assertEquals("Rua A, Campinas", formatarEndereco(PartesEndereco(rua = "Rua A", cidade = "Campinas", numero = " ")))
    }
    @Test fun semRuaUsaLinhaCompleta() {
        assertEquals("Rodovia X, km 10", formatarEndereco(PartesEndereco(linhaCompleta = "Rodovia X, km 10")))
        assertNull(formatarEndereco(PartesEndereco()))
    }
    @Test fun prefereResultadoBrasileiro() {
        val resultados = listOf(
            PartesEndereco(rua = "Calle 1", codigoPais = "AR"),
            PartesEndereco(rua = "Rua 2", codigoPais = "br")
        )
        assertEquals(EnderecoEncontrado("Rua 2", true), escolherEndereco(resultados))
        assertEquals(EnderecoEncontrado("Calle 1", false), escolherEndereco(resultados.take(1)))
        assertNull(escolherEndereco(emptyList()))
    }
    @Test fun lerCoordenadaAceitaVirgula() {
        assertEquals(-23.55, lerCoordenada(" -23,55 ")!!, 0.0)
        assertNull(lerCoordenada("abc"))
    }
    @Test fun validaFaixas() {
        assertTrue(Coordenadas(-23.5, -46.6).validas())
        assertFalse(Coordenadas(91.0, 0.0).validas())
        assertFalse(Coordenadas(Double.NaN, 0.0).validas())
    }
}
