package com.example.marketplace.ui.mapa

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun localizarEndereco(context: Context, endereco: String): Coordenadas? {
    if (!Geocoder.isPresent()) throw IOException("Geocoder indisponível neste dispositivo")
    val geocoder = Geocoder(context.applicationContext, Locale("pt", "BR"))
    val resultados = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine<List<Address>> { continuation ->
            geocoder.getFromLocationName(endereco, 5, -34.0, -74.0, 6.0, -28.0, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }
                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IOException(errorMessage ?: "Falha no Geocoder"))
                    }
                }
            })
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(endereco, 5, -34.0, -74.0, 6.0, -28.0).orEmpty()
        }
    }
    return selecionarEnderecoBrasileiro(resultados.map {
        ResultadoGeocodificacao(
            Coordenadas(if (it.hasLatitude()) it.latitude else Double.NaN,
                if (it.hasLongitude()) it.longitude else Double.NaN), it.countryCode)
    })
}
