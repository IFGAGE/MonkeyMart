package com.example.marketplace.ui.mapa

data class ResultadoGeocodificacao(val coordenadas: Coordenadas, val codigoPais: String?)

class EnderecoForaDoBrasilException : IllegalArgumentException()

fun selecionarEnderecoBrasileiro(resultados: List<ResultadoGeocodificacao>): Coordenadas? {
    // Uma caixa geográfica também contém países vizinhos; o país do resultado
    // é obrigatório para confirmar o endereço, independentemente do idioma.
    val brasileiros = resultados.filter { it.codigoPais?.trim().equals("BR", ignoreCase = true) }
    if (resultados.isNotEmpty() && brasileiros.isEmpty()) throw EnderecoForaDoBrasilException()
    return brasileiros.firstOrNull {
        it.coordenadas.latitude.isFinite() && it.coordenadas.longitude.isFinite() &&
            it.coordenadas.latitude in -90.0..90.0 && it.coordenadas.longitude in -180.0..180.0
    }?.coordenadas
}
