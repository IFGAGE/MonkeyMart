package com.example.rotaspoc

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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Servicos(context: Context) {
    private val context = context.applicationContext

    suspend fun buscarEndereco(texto: String): Ponto = withTimeout(15_000) {
        if (!Geocoder.isPresent()) throw IOException("A busca de endereços não está disponível neste aparelho.")
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        val resultados = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
                geocoder.getFromLocationName(texto, 5, -34.0, -74.0, 6.0, -28.0,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }
                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resumeWithException(
                                IOException("Não foi possível buscar o endereço. Verifique a conexão e tente novamente."))
                        }
                    })
            }
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(texto, 5, -34.0, -74.0, 6.0, -28.0).orEmpty()
        }
        val encontrado = resultados.firstOrNull {
            it.countryCode.equals("BR", ignoreCase = true) && it.hasLatitude() && it.hasLongitude() &&
                Coordenadas(it.latitude, it.longitude).validas()
        } ?: throw IOException("Endereço não encontrado no Brasil. Tente ser mais específico: inclua número, cidade e estado.")
        Ponto(encontrado.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: texto,
            Coordenadas(encontrado.latitude, encontrado.longitude))
    }

    @SuppressLint("MissingPermission")
    suspend fun localizarAtual(): Coordenadas = withTimeout(20_000) {
        val preciso = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val aproximado = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!preciso && !aproximado) throw IOException("Permita o acesso à localização para gerar a rota.")
        val manager = context.getSystemService(LocationManager::class.java)
        val provedores = (if (preciso) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)).filter { manager.isProviderEnabled(it) }
        if (provedores.isEmpty()) throw IOException("Ative a localização do aparelho e tente novamente.")
        for (provedor in provedores) {
            val local = withTimeoutOrNull(8_000) { obterLocalizacao(manager, provedor) }
            if (local != null) return@withTimeout Coordenadas(local.latitude, local.longitude)
        }
        throw IOException("Não foi possível obter sua localização atual. Tente novamente em um local com sinal.")
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

    suspend fun gerarRota(origem: Coordenadas, pontos: List<Ponto>): Rota = withContext(Dispatchers.Default) {
        require(origem.validas() && pontos.size >= 2 && pontos.all { it.coordenadas.validas() }) {
            "Informe pelo menos dois endereços válidos."
        }
        val matrizJson = consultarOsrm("table", listOf(origem) + pontos.map { it.coordenadas }, "annotations=duration")
            .getJSONArray("durations")
        val matriz = (0 until matrizJson.length()).map { linha ->
            val valores = matrizJson.getJSONArray(linha)
            (0 until valores.length()).map { coluna -> if (valores.isNull(coluna)) null else valores.getDouble(coluna) }
        }
        val ordem = ordenarParadas(matriz)
        check(ordem.size == pontos.size) { "O serviço retornou uma rota incompleta. Tente novamente." }
        val paradas = ordem.map { pontos[it] }
        val resposta = consultarOsrm("route", listOf(origem) + paradas.map { it.coordenadas },
            "geometries=geojson&overview=simplified&steps=false")
        val calculada = resposta.getJSONArray("routes").optJSONObject(0)
            ?: throw IOException("Nenhum caminho foi encontrado para esses pontos.")
        val geometria = calculada.getJSONObject("geometry").getJSONArray("coordinates").paraCoordenadas()
        check(geometria.size >= 2 && geometria.all { it.validas() }) { "O serviço retornou um trajeto inválido." }
        Rota(origem, paradas, geometria, calculada.getDouble("distance"), calculada.getDouble("duration"))
    }

    private suspend fun consultarOsrm(servico: String, pontos: List<Coordenadas>, parametros: String): JSONObject =
        withContext(Dispatchers.IO) {
            val coordenadas = pontos.joinToString(";") { "${it.longitude},${it.latitude}" }
            val conexao = URL("https://router.project-osrm.org/$servico/v1/driving/$coordenadas?$parametros")
                .openConnection() as HttpURLConnection
            try {
                conexao.connectTimeout = 15_000
                conexao.readTimeout = 20_000
                conexao.setRequestProperty("User-Agent", "RotasPoc/1.0")
                if (conexao.responseCode !in 200..299) throw IOException("O serviço de rotas está indisponível. Tente novamente.")
                val json = JSONObject(conexao.inputStream.bufferedReader().use { it.readText() })
                ensureActive()
                if (json.optString("code") != "Ok") throw IOException("Não foi possível conectar as paradas pelas ruas. Confira os pontos no mapa.")
                json
            } finally { conexao.disconnect() }
        }
}

private fun JSONArray.paraCoordenadas() = (0 until length()).map {
    val ponto = getJSONArray(it)
    Coordenadas(ponto.getDouble(1), ponto.getDouble(0))
}
