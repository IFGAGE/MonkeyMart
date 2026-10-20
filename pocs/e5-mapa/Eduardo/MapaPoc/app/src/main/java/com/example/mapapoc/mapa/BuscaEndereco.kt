package com.example.mapapoc.mapa

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

data class Coordenadas(val latitude: Double, val longitude: Double)

sealed class EstadoBuscaEndereco {
    object Inicial : EstadoBuscaEndereco()
    object Carregando : EstadoBuscaEndereco()
    data class Encontrado(val endereco: String, val coordenadas: Coordenadas) : EstadoBuscaEndereco()
    data class Erro(val mensagem: String) : EstadoBuscaEndereco()
}

// Mantém o endereço pesquisado junto das coordenadas, mesmo se o campo for editado.
suspend fun buscarEndereco(
    endereco: String,
    resolver: suspend (String) -> Coordenadas?,
    timeoutMillis: Long = 15_000
): EstadoBuscaEndereco {
    val consulta = endereco.trim()
    if (consulta.isBlank()) return EstadoBuscaEndereco.Erro("Informe um endereço.")
    return try {
        val ponto = withTimeout(timeoutMillis) { resolver(consulta) }
        if (ponto == null) {
            EstadoBuscaEndereco.Erro("Endereço não encontrado. Inclua número, cidade e estado.")
        } else {
            EstadoBuscaEndereco.Encontrado(consulta, ponto)
        }
    } catch (e: TimeoutCancellationException) {
        EstadoBuscaEndereco.Erro("A busca demorou demais. Tente novamente.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        EstadoBuscaEndereco.Erro("Não foi possível consultar o endereço. Verifique a conexão e tente novamente.")
    }
}
