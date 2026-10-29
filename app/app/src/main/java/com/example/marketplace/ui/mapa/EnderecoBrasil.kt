package com.example.marketplace.ui.mapa

data class ResultadoGeocodificacao(val coordenadas: Coordenadas, val codigoPais: String?)

class EnderecoForaDoBrasilException : IllegalArgumentException()

fun selecionarEnderecoBrasileiro(resultados: List<ResultadoGeocodificacao>): Coordenadas? {
    
    val brasileiros = resultados.filter { it.codigoPais?.trim().equals("BR", ignoreCase = true) }
    if (resultados.isNotEmpty() && brasileiros.isEmpty()) throw EnderecoForaDoBrasilException()
    return brasileiros.firstOrNull {
        it.coordenadas.latitude.isFinite() && it.coordenadas.longitude.isFinite() &&
            it.coordenadas.latitude in -90.0..90.0 && it.coordenadas.longitude in -180.0..180.0
    }?.coordenadas
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

fun selecionarEnderecoReversoBrasileiro(resultados: List<PartesEndereco>): String? {
    val brasileiros = resultados.filter { it.codigoPais?.trim().equals("BR", ignoreCase = true) }
    if (resultados.isNotEmpty() && brasileiros.isEmpty()) throw EnderecoForaDoBrasilException()
    return brasileiros.firstNotNullOfOrNull { formatarEndereco(it) }
}

private fun String?.limpo() = this?.trim()?.takeIf { it.isNotEmpty() }
