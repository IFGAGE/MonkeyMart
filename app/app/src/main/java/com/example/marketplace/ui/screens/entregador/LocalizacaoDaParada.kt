package com.example.marketplace.ui.screens.entregador

import android.location.Location
import android.os.SystemClock
import com.example.marketplace.ui.acompanhamento.coordenadaValida
import com.example.marketplace.ui.acompanhamento.distanciaMetros
import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.proximidade.MonitorGeofence

internal const val VALIDADE_POSICAO_MS = 30_000L

internal fun Location.idadeMillis(): Long =
    (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L

internal fun Location.distanciaAteParada(destino: Coordenadas): Double? =
    distanciaAteParada(Coordenadas(latitude, longitude), destino, accuracy.takeIf { hasAccuracy() }, idadeMillis())

internal fun distanciaAteParada(
    origem: Coordenadas,
    destino: Coordenadas,
    precisao: Float?,
    idadeMillis: Long
): Double? {
    if (precisao == null || precisao !in 0f..100f || idadeMillis !in 0L until VALIDADE_POSICAO_MS) return null
    if (!coordenadaValida(origem.latitude, origem.longitude) ||
        !coordenadaValida(destino.latitude, destino.longitude)) return null
    return distanciaMetros(origem, destino)
}

internal fun Double?.dentroDoRaioDaParada(): Boolean = this != null && this <= MonitorGeofence.RAIO
