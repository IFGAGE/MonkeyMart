package com.example.marketplace.ui.mapa

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.screens.entregador.PedidoTemp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

data class ParadaRota(
    val pedidoId: Int,
    val endereco: String,
    val coordenadas: Coordenadas
)

data class RotaGerada(
    val origem: Coordenadas,
    val paradas: List<ParadaRota>,
    val geometria: List<Coordenadas>
)

@SuppressLint("MissingPermission")
suspend fun localizarAtual(context: Context): Coordenadas = try {
    withTimeout(17_000) {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val provedoresPermitidos = if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val providers = provedoresPermitidos
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) throw IOException("Ative a localização do dispositivo.")

        providers.forEach { provider ->
            val location = withTimeoutOrNull(8_000) {
                solicitarLocalizacao(context, locationManager, provider)
            }
            if (location != null) {
                return@withTimeout Coordenadas(location.latitude, location.longitude)
            }
        }
        throw IOException("Não foi possível obter a localização atual.")
    }
} catch (e: TimeoutCancellationException) {
    throw IOException("A localização demorou demais. Tente novamente.", e)
}

@SuppressLint("MissingPermission")
private suspend fun solicitarLocalizacao(
    context: Context,
    locationManager: LocationManager,
    provider: String
): Location? = suspendCancellableCoroutine { continuation ->
    val cancellationSignal = CancellationSignal()
    continuation.invokeOnCancellation { cancellationSignal.cancel() }
    LocationManagerCompat.getCurrentLocation(
        locationManager,
        provider,
        cancellationSignal,
        ContextCompat.getMainExecutor(context)
    ) { location ->
        if (continuation.isActive) continuation.resume(location)
    }
}

suspend fun gerarRota(context: Context, pedidos: List<PedidoTemp>): RotaGerada {
    require(pedidos.isNotEmpty()) { "Selecione ao menos um pedido." }
    val origem = localizarAtual(context)
    val paradas = pedidos.map { pedido ->
        val coordenadas = when (
            val resultado = buscarEndereco(pedido.enderecoEntrega, { localizarEndereco(context, it) })
        ) {
            is EstadoBuscaEndereco.Encontrado -> resultado.coordenadas
            is EstadoBuscaEndereco.Erro -> throw IOException("Pedido #${pedido.id}: ${resultado.mensagem}")
            else -> throw IOException("Pedido #${pedido.id}: endereço não encontrado.")
        }
        ParadaRota(pedido.id, pedido.enderecoEntrega, coordenadas)
    }
    val pontos = listOf(origem) + paradas.map { it.coordenadas }
    val matriz = consultarOsrm("table", pontos, "annotations=duration").getJSONArray("durations")
    val ordem = ordenarParadas(matriz, paradas.size)
    val paradasOrdenadas = ordem.map { paradas[it - 1] }
    val pontosOrdenados = listOf(origem) + paradasOrdenadas.map { it.coordenadas }
    val respostaRota = consultarOsrm(
        "route",
        pontosOrdenados,
        "geometries=geojson&overview=full&steps=false"
    )
    val coordenadasJson = respostaRota.getJSONArray("routes")
        .takeIf { it.length() > 0 }
        ?.getJSONObject(0)
        ?.getJSONObject("geometry")
        ?.getJSONArray("coordinates")
        ?: throw IOException("O serviço não retornou um trajeto.")
    val geometria = coordenadasJson.toCoordenadas()
    if (geometria.size < 2) throw IOException("O trajeto retornado é inválido.")
    return RotaGerada(origem, paradasOrdenadas, geometria)
}

private fun ordenarParadas(matriz: JSONArray, quantidade: Int): List<Int> {
    require(matriz.length() == quantidade + 1) { "Matriz de rota inválida." }
    val restantes = (1..quantidade).toMutableSet()
    val ordem = mutableListOf<Int>()
    var atual = 0
    while (restantes.isNotEmpty()) {
        val duracoes = matriz.getJSONArray(atual)
        val proximo = restantes.minByOrNull { indice ->
            if (duracoes.isNull(indice)) Double.POSITIVE_INFINITY else duracoes.getDouble(indice)
        } ?: throw IOException("Não há rota entre os endereços selecionados.")
        if (duracoes.isNull(proximo) || !duracoes.getDouble(proximo).isFinite()) {
            throw IOException("Não há rota entre os endereços selecionados.")
        }
        ordem += proximo
        restantes -= proximo
        atual = proximo
    }
    return ordem
}

private suspend fun consultarOsrm(
    servico: String,
    pontos: List<Coordenadas>,
    parametros: String
): JSONObject = withContext(Dispatchers.IO) {
    pontos.forEach { validarCoordenadas(it) }
    val coordenadas = pontos.joinToString(";") { "${it.longitude},${it.latitude}" }
    val conexao = URL("https://router.project-osrm.org/$servico/v1/driving/$coordenadas?$parametros")
        .openConnection() as HttpURLConnection
    try {
        conexao.connectTimeout = 15_000
        conexao.readTimeout = 20_000
        conexao.setRequestProperty("User-Agent", "MonkeyMart/1.0")
        if (conexao.responseCode !in 200..299) {
            throw IOException("Serviço de rotas indisponível (${conexao.responseCode}).")
        }
        val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
        JSONObject(resposta).also {
            if (it.optString("code") != "Ok") throw IOException("Não foi possível gerar a rota.")
        }
    } finally {
        conexao.disconnect()
    }
}

fun RotaGerada.paraEntity(emailEntregador: String) = RotaEntity(
    emailEntregador = emailEntregador,
    pedidosJson = JSONArray(paradas.map { it.pedidoId }).toString(),
    origemJson = origem.toJson().toString(),
    destinosJson = JSONArray().apply {
        paradas.forEach { parada ->
            put(JSONObject().apply {
                put("pedidoId", parada.pedidoId)
                put("endereco", parada.endereco)
                put("coordenadas", parada.coordenadas.toJson())
            })
        }
    }.toString(),
    geometriaJson = JSONArray().apply {
        geometria.forEach { put(JSONArray().put(it.longitude).put(it.latitude)) }
    }.toString()
)

fun RotaEntity.paraRotaGerada(): RotaGerada {
    val origem = JSONObject(origemJson).toCoordenadas()
    val destinos = JSONArray(destinosJson)
    val paradas = (0 until destinos.length()).map { indice ->
        val parada = destinos.getJSONObject(indice)
        ParadaRota(
            pedidoId = parada.getInt("pedidoId"),
            endereco = parada.getString("endereco"),
            coordenadas = parada.getJSONObject("coordenadas").toCoordenadas()
        )
    }
    val geometria = JSONArray(geometriaJson).toCoordenadas()
    require(
        paradas.isNotEmpty() && geometria.size >= 2 && pedidoIds() == paradas.map { it.pedidoId }
    ) { "Dados da rota inválidos." }
    return RotaGerada(origem, paradas, geometria)
}

fun RotaEntity.pedidoIds(): List<Int> {
    val ids = JSONArray(pedidosJson)
    return (0 until ids.length()).map { ids.getInt(it) }
}

private fun Coordenadas.toJson() = JSONObject().apply {
    validarCoordenadas(this@toJson)
    put("latitude", latitude)
    put("longitude", longitude)
}

private fun JSONObject.toCoordenadas() = Coordenadas(
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude")
).also(::validarCoordenadas)

private fun JSONArray.toCoordenadas(): List<Coordenadas> = (0 until length()).map { indice ->
    val item = getJSONArray(indice)
    Coordenadas(latitude = item.getDouble(1), longitude = item.getDouble(0)).also(::validarCoordenadas)
}

private fun validarCoordenadas(coordenadas: Coordenadas) {
    require(
        coordenadas.latitude.isFinite() && coordenadas.latitude in -90.0..90.0 &&
            coordenadas.longitude.isFinite() && coordenadas.longitude in -180.0..180.0
    ) { "Coordenadas inválidas." }
}
