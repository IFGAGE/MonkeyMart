package com.example.marketplace.ui.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.example.marketplace.R
import android.os.SystemClock
import android.util.Log

@Composable
fun MapaDestino(resultado: EstadoBuscaEndereco.Encontrado, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val map = lembrarMapa()
    val marker = remember(map) {
        Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_pin_monkey)
            map.overlays.add(this)
        }
    }
    GerenciarCicloDeVida(map)
    AndroidView(
        factory = { map },
        update = { view ->
            val ponto = GeoPoint(resultado.coordenadas.latitude, resultado.coordenadas.longitude)
            if (marker.position != ponto || marker.title != resultado.endereco) {
                marker.position = ponto
                marker.title = resultado.endereco
                view.controller.setCenter(ponto)
                marker.showInfoWindow()
                view.invalidate()
            }
        },
        modifier = modifier.clipToBounds()
    )
}

@Composable
fun lembrarEstadoMapaRota(): EstadoMapaRota {
    return remember { EstadoMapaRota() }
}

class EstadoMapaRota internal constructor() {
    internal var camera: CameraMapaRota? = null
}

internal data class CameraMapaRota(val rota: RotaGerada, val centro: Coordenadas, val zoom: Double)

private fun salvarCamera(view: MapaRotaView, estado: EstadoMapaRota) {
    val rota = view.rotaDesenhada ?: return
    if (!view.enquadrado) return
    val centro = view.mapCenter
    estado.camera = CameraMapaRota(rota, Coordenadas(centro.latitude, centro.longitude), view.zoomLevelDouble)
}

@Composable
fun MapaRota(rota: RotaGerada, modifier: Modifier = Modifier,
    estado: EstadoMapaRota = lembrarEstadoMapaRota()) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        factory = { context -> criarMapa(context) { MapaRotaView(context) } },
        onReset = { view -> salvarCamera(view, estado); view.prepararReuso() },
        onRelease = { view -> salvarCamera(view, estado); view.liberar() },
        update = atualizar@{ view ->
            view.acompanharCiclo(lifecycle)
            // O update também roda após uma reativação da lista. A geometria
            // pode continuar igual, mas a View precisa voltar a desenhar.
            view.postInvalidateOnAnimation()
            if (view.rotaDesenhada === rota) return@atualizar
            val inicio = SystemClock.elapsedRealtime()
            view.overlays.removeAll { it !is CopyrightOverlay }

            view.overlays.add(Polyline().apply {
                setPoints(rota.geometria.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.rgb(33, 150, 243)
                outlinePaint.strokeWidth = 12f
            })
            view.overlays.add(Marker(view).apply {
                position = GeoPoint(rota.origem.latitude, rota.origem.longitude)
                title = "Localização atual"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            rota.paradas.forEachIndexed { indice, parada ->
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(parada.coordenadas.latitude, parada.coordenadas.longitude)
                    title = "${indice + 1}. ${if (parada.tipo == TipoParada.COLETA) "Coleta" else "Entrega"}"
                    snippet = parada.endereco + "\nPedidos " + parada.pedidoIds.joinToString { "#$it" }
                    icon = iconeNumerado(view.context, indice + 1)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            val pontos = listOf(rota.origem) + rota.paradas.map { it.coordenadas }
            view.rotaDesenhada = rota
            view.enquadrado = false
            view.doOnLayout {
                if (view.rotaDesenhada === rota) {
                    val camera = estado.camera?.takeIf { it.rota === rota }
                    if (camera != null) {
                        view.controller.setZoom(camera.zoom)
                        view.controller.setCenter(GeoPoint(camera.centro.latitude, camera.centro.longitude))
                    } else view.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(pontos.map { GeoPoint(it.latitude, it.longitude) }), false, 80)
                    view.enquadrado = true
                }
            }
            view.invalidate()
            Log.d("RotaDesempenho", "Desenho preparado em ${SystemClock.elapsedRealtime() - inicio}ms; ${rota.geometria.size} pontos")
        },
        modifier = modifier.clipToBounds()
    )
}

private fun iconeNumerado(context: Context, numero: Int): Drawable {
    val densidade = context.resources.displayMetrics.density
    val tamanho = (40 * densidade).toInt()
    val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(33, 150, 243) }
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 18 * densidade
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val centro = tamanho / 2f
    canvas.drawCircle(centro, centro, centro, fundo)
    canvas.drawText(numero.toString(), centro, centro - (texto.ascent() + texto.descent()) / 2, texto)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
private fun lembrarMapa(): MapView {
    val context = LocalContext.current
    return remember(context) { criarMapa(context) { MapaLimitado(context) } }
}

private fun <T : MapView> criarMapa(context: Context, factory: () -> T): T {
    val inicio = SystemClock.elapsedRealtime()
    val appContext = context.applicationContext
    if (!configuracaoCarregada) {
        Configuration.getInstance().load(appContext, PreferenceManager.getDefaultSharedPreferences(appContext))
        Configuration.getInstance().userAgentValue = "MonkeyMart/1.0"
        configuracaoCarregada = true
    }
    return factory().apply {
        setDestroyMode(false)
        setTileSource(
            XYTileSource(
                "OpenStreetMap",
                0,
                19,
                256,
                ".png",
                arrayOf("https://tile.openstreetmap.org/"),
                "© OpenStreetMap contributors"
            )
        )
        setMultiTouchControls(true)
        controller.setZoom(18.0)
        overlays.add(CopyrightOverlay(context))
        Log.d("RotaDesempenho", "Mapa criado em ${SystemClock.elapsedRealtime() - inicio}ms")
    }
}

// Criação de Views e composição ocorrem na thread principal.
private var configuracaoCarregada = false

@Composable
private fun GerenciarCicloDeVida(map: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(map, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }
}
