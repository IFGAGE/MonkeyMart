package com.example.marketplace.ui.mapa

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BuscaEnderecoTest {
    @Test fun enderecoVazioNaoConsultaServico() = runBlocking {
        val resultado = buscarEndereco("   ", { fail("Não deve consultar"); null })
        assertTrue(resultado is EstadoBuscaEndereco.Erro)
    }
    @Test fun sucessoPreservaEnderecoPesquisadoECoordenadas() = runBlocking {
        val resultado = buscarEndereco("  Rua A, 10, São Carlos  ", {
            assertEquals("Rua A, 10, São Carlos", it)
            Coordenadas(-22.0, -47.0)
        }) as EstadoBuscaEndereco.Encontrado
        assertEquals("Rua A, 10, São Carlos", resultado.endereco)
        assertEquals(Coordenadas(-22.0, -47.0), resultado.coordenadas)
    }
    @Test fun falhaDepoisDeSucessoNaoMantemPontoAntigo() = runBlocking {
        var estado = buscarEndereco("Rua A", { Coordenadas(-22.0, -47.0) })
        assertTrue(estado is EstadoBuscaEndereco.Encontrado)
        estado = buscarEndereco("endereço inexistente", { null })
        assertTrue(estado is EstadoBuscaEndereco.Erro)
    }
    @Test fun falhaDeRedeViraMensagem() = runBlocking {
        assertTrue(buscarEndereco("Rua A", { throw java.io.IOException("offline") }) is EstadoBuscaEndereco.Erro)
    }
    @Test fun buscaLentaTemLimite() = runBlocking {
        val resultado = buscarEndereco("Rua A", { delay(1000); null }, timeoutMillis = 20)
        assertTrue((resultado as EstadoBuscaEndereco.Erro).mensagem.contains("demorou"))
    }
    @Test fun cancelamentoNaoViraErroDeEndereco() = runBlocking {
        try {
            buscarEndereco("Rua A", { throw CancellationException("saiu da tela") })
            fail("Deve preservar cancelamento")
        } catch (_: CancellationException) { }
    }
    @Test fun novaBuscaUsaNovoEndereco() = runBlocking {
        val primeiro = buscarEndereco("Rua A", { Coordenadas(1.0, 2.0) })
        val segundo = buscarEndereco("Rua B", { Coordenadas(3.0, 4.0) }) as EstadoBuscaEndereco.Encontrado
        assertNotEquals(primeiro, segundo)
        assertEquals("Rua B", segundo.endereco)
        assertEquals(Coordenadas(3.0, 4.0), segundo.coordenadas)
    }
}
