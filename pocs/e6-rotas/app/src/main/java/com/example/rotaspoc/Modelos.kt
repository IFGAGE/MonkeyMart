package com.example.rotaspoc

data class Coordenadas(val latitude: Double, val longitude: Double) {
    fun validas() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class Ponto(val endereco: String, val coordenadas: Coordenadas)

data class CampoEndereco(val id: Int, val texto: String = "", val buscando: Boolean = false,
    val ponto: Ponto? = null, val erro: String? = null) {
    fun comTexto(novoTexto: String) = copy(texto = novoTexto, buscando = false, ponto = null, erro = null)
}

fun pontosValidos(campos: List<CampoEndereco>): List<Ponto> = campos.mapNotNull {
    it.ponto?.takeIf { ponto -> !it.buscando && it.erro == null && ponto.coordenadas.validas() }
}

data class Rota(val origem: Coordenadas, val paradas: List<Ponto>, val geometria: List<Coordenadas>,
    val distanciaMetros: Double, val duracaoSegundos: Double)

sealed class Painel {
    data class Endereco(val ponto: Ponto) : Painel()
    data class Gerando(val mensagem: String) : Painel()
    data class Gerada(val rota: Rota, val ignorados: Int) : Painel()
    data class Erro(val mensagem: String) : Painel()
}

data class EstadoTela(val campos: List<CampoEndereco> = listOf(CampoEndereco(1), CampoEndereco(2)),
    val painel: Painel? = null, val erro: String? = null) {
    val gerando get() = painel is Painel.Gerando
}
