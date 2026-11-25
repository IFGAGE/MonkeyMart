package com.example.marketplace.ui.acompanhamento

import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.ui.mapa.Coordenadas
import kotlin.math.*

internal data class PosicaoEntrega(
    val latitude: Double,
    val longitude: Double,
    val precisao: Float,
    val capturadaEm: Long,
    val emailEntregador: String,
    val destinoLatitude: Double,
    val destinoLongitude: Double,
    val simulada: Boolean = false
) {
    val coordenadas get() = Coordenadas(latitude, longitude)
    fun distanciaMetros(): Double = distanciaMetros(coordenadas, Coordenadas(destinoLatitude, destinoLongitude))
    fun atual(agora: Long) = agora - capturadaEm in 0L..30_000L
    fun corresponde(pedido: PedidoEntity) = pedido.statusEntrega == "EM_ROTA" &&
        pedido.emailEntregador == emailEntregador && pedido.entregaLatitude == destinoLatitude &&
        pedido.entregaLongitude == destinoLongitude
    fun dados(): Map<String, Any> = mapOf("latitude" to latitude, "longitude" to longitude,
        "precisao" to precisao, "capturadaEm" to capturadaEm, "emailEntregador" to emailEntregador,
        "destinoLatitude" to destinoLatitude, "destinoLongitude" to destinoLongitude)
}

internal fun lerPosicao(dados: Map<*, *>?): PosicaoEntrega? {
    if (dados == null) return null
    val latitude = (dados["latitude"] as? Number)?.toDouble() ?: return null
    val longitude = (dados["longitude"] as? Number)?.toDouble() ?: return null
    val precisao = (dados["precisao"] as? Number)?.toFloat() ?: return null
    val instante = (dados["capturadaEm"] as? Number)?.toLong() ?: return null
    val entregador = (dados["emailEntregador"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val destinoLat = (dados["destinoLatitude"] as? Number)?.toDouble() ?: return null
    val destinoLon = (dados["destinoLongitude"] as? Number)?.toDouble() ?: return null
    if (!coordenadaValida(latitude, longitude) || !coordenadaValida(destinoLat, destinoLon) ||
        !precisao.isFinite() || precisao !in 0f..100f || instante <= 0) return null
    return PosicaoEntrega(latitude, longitude, precisao, instante, entregador, destinoLat, destinoLon)
}

internal fun coordenadaValida(latitude: Double, longitude: Double) =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

internal fun distanciaMetros(origem: Coordenadas, destino: Coordenadas): Double {
    val lat1 = Math.toRadians(origem.latitude)
    val lat2 = Math.toRadians(destino.latitude)
    val dlon = Math.toRadians(destino.longitude - origem.longitude)
    val a = (sin((lat2 - lat1) / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)).coerceIn(0.0, 1.0)
    return 6_371_000 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun podeRegistrarAproximacao(posicao: PosicaoEntrega, agora: Long): Boolean =
    posicao.atual(agora) && coordenadaValida(posicao.latitude, posicao.longitude) &&
        coordenadaValida(posicao.destinoLatitude, posicao.destinoLongitude) &&
        posicao.precisao.isFinite() && posicao.precisao in 0f..100f && posicao.distanciaMetros() <= 200.0
