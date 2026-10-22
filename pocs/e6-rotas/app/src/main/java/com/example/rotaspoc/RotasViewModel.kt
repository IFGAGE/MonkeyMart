package com.example.rotaspoc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RotasViewModel(application: Application) : AndroidViewModel(application) {
    private val servicos = Servicos(application)
    private val estadoInterno = MutableStateFlow(EstadoTela())
    val estado = estadoInterno.asStateFlow()
    private val buscas = mutableMapOf<Int, Job>()
    private var proximoId = 3
    private var geracao: Job? = null

    fun alterarEndereco(id: Int, texto: String) {
        buscas.remove(id)?.cancel()
        estadoInterno.update { tela -> tela.copy(erro = null,
            campos = tela.campos.map { if (it.id == id) it.comTexto(texto) else it }) }
        if (texto.isBlank()) return
        buscas[id] = viewModelScope.launch {
            delay(700)
            atualizarCampo(id) { it.copy(buscando = true) }
            try {
                val ponto = servicos.buscarEndereco(texto.trim())
                atualizarCampo(id) { if (it.texto == texto) it.copy(buscando = false, ponto = ponto, erro = null) else it }
            } catch (e: TimeoutCancellationException) {
                atualizarCampo(id) { if (it.texto == texto) it.copy(buscando = false, erro = "A busca demorou demais. Tente digitar o endereço novamente.") else it }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                atualizarCampo(id) { if (it.texto == texto) it.copy(buscando = false, erro = e.message ?: "Não foi possível encontrar esse endereço.") else it }
            }
        }
    }

    fun adicionarParada() {
        val campo = CampoEndereco(proximoId++)
        estadoInterno.update { it.copy(campos = it.campos + campo, erro = null) }
    }

    fun removerParada(id: Int) {
        if (estado.value.campos.size <= 2) return
        buscas.remove(id)?.cancel()
        estadoInterno.update { it.copy(campos = it.campos.filterNot { campo -> campo.id == id }, erro = null) }
    }

    fun verEndereco(id: Int) {
        estado.value.campos.find { it.id == id }?.ponto?.let { ponto ->
            estadoInterno.update { it.copy(painel = Painel.Endereco(ponto)) }
        }
    }

    fun permissaoNegada() {
        estadoInterno.update { it.copy(erro = "Permita o acesso à localização nas configurações do aplicativo para gerar a rota.") }
    }

    fun gerarRota() {
        if (estado.value.gerando) return
        val campos = estado.value.campos
        val pontos = pontosValidos(campos)
        if (pontos.size < 2) {
            estadoInterno.update { it.copy(erro = "É preciso localizar pelo menos dois endereços no mapa.") }
            return
        }
        estadoInterno.update { it.copy(painel = Painel.Gerando("Obtendo sua localização atual…"), erro = null) }
        geracao = viewModelScope.launch {
            try {
                val origem = servicos.localizarAtual()
                estadoInterno.update { it.copy(painel = Painel.Gerando("Calculando a ordem das paradas e o caminho pelas ruas…")) }
                val rota = withTimeout(60_000) { servicos.gerarRota(origem, pontos) }
                estadoInterno.update { it.copy(painel = Painel.Gerada(rota, campos.size - pontos.size)) }
            } catch (e: TimeoutCancellationException) {
                estadoInterno.update { it.copy(painel = Painel.Erro("A geração demorou demais. Confira a localização e a conexão e tente novamente.")) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                estadoInterno.update { it.copy(painel = Painel.Erro(e.message ?: "Não foi possível gerar a rota. Tente novamente.")) }
            }
        }
    }

    fun fecharPainel() {
        geracao?.cancel()
        geracao = null
        estadoInterno.update { it.copy(painel = null) }
    }

    private fun atualizarCampo(id: Int, transformar: (CampoEndereco) -> CampoEndereco) {
        estadoInterno.update { it.copy(campos = it.campos.map { campo -> if (campo.id == id) transformar(campo) else campo }) }
    }
}
