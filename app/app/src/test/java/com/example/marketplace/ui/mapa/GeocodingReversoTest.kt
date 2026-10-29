package com.example.marketplace.ui.mapa

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeocodingReversoTest {
    @Test fun formataEnderecoCompleto() {
        val partes = PartesEndereco("Avenida Paulista", "1578", "Bela Vista", "São Paulo", "SP", "01310-200", "BR")
        assertEquals("Avenida Paulista, 1578 - Bela Vista, São Paulo - SP, 01310-200", formatarEndereco(partes))
    }
    @Test fun camposAusentesSaoOmitidos() {
        assertEquals("Rua A, Campinas", formatarEndereco(PartesEndereco(rua = "Rua A", numero = " ", cidade = "Campinas")))
    }
    @Test fun semRuaUsaLinhaCompleta() {
        assertEquals("Rodovia X, km 10", formatarEndereco(PartesEndereco(linhaCompleta = "Rodovia X, km 10")))
        assertNull(formatarEndereco(PartesEndereco()))
    }
    @Test fun escolheResultadoBrasileiro() {
        val resultados = listOf(PartesEndereco(rua = "Calle 1", codigoPais = "AR"), PartesEndereco(rua = "Rua 2", codigoPais = "br"))
        assertEquals("Rua 2", selecionarEnderecoReversoBrasileiro(resultados))
        assertNull(selecionarEnderecoReversoBrasileiro(emptyList()))
    }
    @Test(expected = EnderecoForaDoBrasilException::class) fun somenteEstrangeiroLancaErro() {
        selecionarEnderecoReversoBrasileiro(listOf(PartesEndereco(rua = "Calle 1", codigoPais = "AR")))
    }
    @Test fun sucessoMantemPontoTocado() = runBlocking {
        val ponto = Coordenadas(-22.0, -47.0)
        val resultado = buscarEnderecoPorPonto(ponto, { " Rua A, 10 " }) as EstadoBuscaEndereco.Encontrado
        assertEquals("Rua A, 10", resultado.endereco)
        assertEquals(ponto, resultado.coordenadas)
    }
    @Test fun pontoInvalidoNaoConsultaServico() = runBlocking {
        val resultado = buscarEnderecoPorPonto(Coordenadas(Double.NaN, 0.0), { fail("Não deve consultar"); null })
        assertTrue(resultado is EstadoBuscaEndereco.Erro)
    }
    @Test fun semEnderecoForaDoBrasilEFalhaViramMensagens() = runBlocking {
        val ponto = Coordenadas(-22.0, -47.0)
        assertTrue(buscarEnderecoPorPonto(ponto, { null }) is EstadoBuscaEndereco.Erro)
        val fora = buscarEnderecoPorPonto(ponto, { throw EnderecoForaDoBrasilException() }) as EstadoBuscaEndereco.Erro
        assertTrue(fora.mensagem.contains("Brasil"))
        assertTrue(buscarEnderecoPorPonto(ponto, { throw java.io.IOException("offline") }) is EstadoBuscaEndereco.Erro)
        val lenta = buscarEnderecoPorPonto(ponto, { delay(1000); "Rua A" }, timeoutMillis = 20) as EstadoBuscaEndereco.Erro
        assertTrue(lenta.mensagem.contains("demorou"))
    }
    @Test fun cancelamentoEhPreservado() = runBlocking {
        try {
            buscarEnderecoPorPonto(Coordenadas(-22.0, -47.0), { throw CancellationException("saiu da tela") })
            fail("Deve preservar cancelamento")
        } catch (_: CancellationException) { }
    }
}
