package com.example.reversapoc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.*
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Servicos(context: Context) {
    private val context = context.applicationContext

    suspend fun enderecoDoPonto(ponto: Coordenadas): EnderecoEncontrado = withTimeout(15_000) {
        if (!ponto.validas()) throw IOException("Coordenadas inválidas.")
        if (!Geocoder.isPresent()) throw IOException("A busca de endereços não está disponível neste aparelho.")
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        val resultados = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
                geocoder.getFromLocation(ponto.latitude, ponto.longitude, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resumeWithException(
                            IOException("Não foi possível consultar o endereço. Verifique a conexão."))
                    }
                })
            }
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(ponto.latitude, ponto.longitude, 5).orEmpty()
        }
        if (resultados.isEmpty()) throw IOException("Nenhum endereço encontrado para este ponto.")
        escolherEndereco(resultados.map { it.paraPartes() })
            ?: throw IOException("Nenhum endereço encontrado para este ponto.")
    }

    @SuppressLint("MissingPermission")
    suspend fun localizarAtual(): Coordenadas = withTimeout(20_000) {
        val preciso = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val aproximado = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!preciso && !aproximado) throw IOException("Permita o acesso à localização.")
        val manager = context.getSystemService(LocationManager::class.java)
        val provedores = (if (preciso) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)).filter { manager.isProviderEnabled(it) }
        if (provedores.isEmpty()) throw IOException("Ative a localização do aparelho e tente novamente.")
        for (provedor in provedores) {
            val local = withTimeoutOrNull(8_000) { obterLocalizacao(manager, provedor) }
            if (local != null) return@withTimeout Coordenadas(local.latitude, local.longitude)
        }
        throw IOException("Não foi possível obter sua localização atual.")
    }

    @SuppressLint("MissingPermission")
    private suspend fun obterLocalizacao(manager: LocationManager, provedor: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancelamento = CancellationSignal()
            continuation.invokeOnCancellation { cancelamento.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provedor, cancelamento,
                ContextCompat.getMainExecutor(context)) { local ->
                if (continuation.isActive) continuation.resume(local)
            }
        }
}

private fun Address.paraPartes() = PartesEndereco(
    rua = thoroughfare,
    numero = subThoroughfare,
    bairro = subLocality,
    cidade = locality ?: subAdminArea,
    estado = adminArea,
    cep = postalCode,
    codigoPais = countryCode,
    linhaCompleta = getAddressLine(0)
)
