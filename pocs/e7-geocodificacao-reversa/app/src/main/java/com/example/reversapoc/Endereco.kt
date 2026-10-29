package com.example.reversapoc

data class Coordenadas(val latitude: Double, val longitude: Double) {
    fun validas() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class PartesEndereco(
    val rua: String? = null,
    val numero: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val estado: String? = null,
    val cep: String? = null,
    val codigoPais: String? = null,
    val linhaCompleta: String? = null
)

fun lerCoordenada(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

fun formatarEndereco(partes: PartesEndereco): String? {
    val rua = partes.rua.limpo() ?: return partes.linhaCompleta.limpo()
    val logradouro = listOfNotNull(rua, partes.numero.limpo()).joinToString(", ")
    val local = listOfNotNull(partes.cidade.limpo(), partes.estado.limpo()).joinToString(" - ")
    return listOfNotNull(
        listOfNotNull(logradouro, partes.bairro.limpo()).joinToString(" - "),
        local.ifBlank { null },
        partes.cep.limpo()
    ).joinToString(", ")
}

data class EnderecoEncontrado(val texto: String, val noBrasil: Boolean)

fun escolherEndereco(resultados: List<PartesEndereco>): EnderecoEncontrado? {
    val (brasileiros, estrangeiros) = resultados.partition { it.codigoPais?.trim().equals("BR", ignoreCase = true) }
    brasileiros.firstNotNullOfOrNull { formatarEndereco(it) }?.let { return EnderecoEncontrado(it, true) }
    return estrangeiros.firstNotNullOfOrNull { formatarEndereco(it) }?.let { EnderecoEncontrado(it, false) }
}

private fun String?.limpo() = this?.trim()?.takeIf { it.isNotEmpty() }
