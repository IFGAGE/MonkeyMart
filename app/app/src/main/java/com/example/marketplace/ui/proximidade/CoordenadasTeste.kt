package com.example.marketplace.ui.proximidade

import com.example.marketplace.ui.mapa.Coordenadas
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal fun pontoAoNorte(destino: Coordenadas, distanciaMetros: Double): Coordenadas {
    val latitude = Math.toRadians(destino.latitude)
    val longitude = Math.toRadians(destino.longitude)
    val arco = distanciaMetros / 6_371_000.0
    val novaLatitude = asin((sin(latitude) * cos(arco) + cos(latitude) * sin(arco)).coerceIn(-1.0, 1.0))
    val novaLongitude = longitude + atan2(0.0, cos(arco) - sin(latitude) * sin(novaLatitude))
    return Coordenadas(Math.toDegrees(novaLatitude), (Math.toDegrees(novaLongitude) + 540.0) % 360.0 - 180.0)
}
