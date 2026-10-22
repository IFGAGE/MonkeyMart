package com.example.rotaspoc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.ViewOutlineProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

private data class ConteudoMapa(val pontos: List<Ponto>, val origem: Coordenadas?, val geometria: List<Coordenadas>)

@Composable
fun Mapa(pontos: List<Ponto>, origem: Coordenadas? = null, geometria: List<Coordenadas> = emptyList(),
    modifier: Modifier = Modifier) {
    val conteudo = remember(pontos, origem, geometria) { ConteudoMapa(pontos, origem, geometria) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(factory = { context -> configurarMapa(context); MapaNativo(context) },
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        onRelease = { it.liberar() },
        update = { view ->
            view.acompanhar(lifecycle)
            if (view.conteudo !== conteudo) view.mostrar(conteudo)
        })
}

// Uma View pertence ao seu AndroidView; não é movida manualmente entre holders.
private class MapaNativo(context: Context) : MapView(context) {
    var conteudo: ConteudoMapa? = null
        private set
    private var lifecycle: Lifecycle? = null
    private var retomado = false
    private var liberado = false
    private val observer = LifecycleEventObserver { _, _ -> atualizarCiclo() }

    init {
        setDestroyMode(false)
        setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors"))
        setMultiTouchControls(true)
        setMaxZoomLevel(19.0)
        controller.setZoom(17.0)
        overlays.add(CopyrightOverlay(context))
        clipChildren = true
        clipToPadding = true
        outlineProvider = ViewOutlineProvider.BOUNDS
        clipToOutline = true
    }

    fun acompanhar(atual: Lifecycle) {
        if (lifecycle !== atual) {
            lifecycle?.removeObserver(observer)
            lifecycle = atual
            atual.addObserver(observer)
        }
        atualizarCiclo()
    }

    private fun atualizarCiclo() {
        val retomar = !liberado && isAttachedToWindow && lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        if (retomar == retomado) return
        retomado = retomar
        if (retomar) onResume() else onPause()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); atualizarCiclo() }
    override fun onDetachedFromWindow() {
        if (retomado) { retomado = false; onPause() }
        super.onDetachedFromWindow()
    }

    fun liberar() {
        if (liberado) return
        liberado = true
        lifecycle?.removeObserver(observer)
        lifecycle = null
        atualizarCiclo()
        onDetach()
    }

    fun mostrar(novo: ConteudoMapa) {
        conteudo = novo
        overlays.removeAll { it !is CopyrightOverlay }
        if (novo.geometria.isNotEmpty()) overlays.add(Polyline().apply {
            setPoints(novo.geometria.map { GeoPoint(it.latitude, it.longitude) })
            outlinePaint.color = Color.rgb(23, 86, 169)
            outlinePaint.strokeWidth = 4 * resources.displayMetrics.density
        })
        novo.origem?.let { origem ->
            overlays.add(Marker(this).apply {
                position = GeoPoint(origem.latitude, origem.longitude)
                title = "Origem: sua localização atual"
                icon = criarPin(context, 0, Color.rgb(31, 122, 75))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
        }
        novo.pontos.forEachIndexed { indice, ponto ->
            overlays.add(Marker(this).apply {
                position = GeoPoint(ponto.coordenadas.latitude, ponto.coordenadas.longitude)
                title = "Parada ${indice + 1}"
                snippet = ponto.endereco
                icon = criarPin(context, indice + 1, Color.rgb(23, 86, 169))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            })
        }
        doOnLayout {
            if (conteudo === novo) {
                val pontos = listOfNotNull(novo.origem) + novo.pontos.map { it.coordenadas } + novo.geometria
                if (pontos.size == 1) controller.setCenter(GeoPoint(pontos[0].latitude, pontos[0].longitude))
                else if (pontos.isNotEmpty()) zoomToBoundingBox(
                    BoundingBox.fromGeoPoints(pontos.map { GeoPoint(it.latitude, it.longitude) }), false,
                    (24 * resources.displayMetrics.density).toInt())
                invalidate()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        val salvo = canvas.save()
        try { canvas.clipRect(0, 0, width, height); super.draw(canvas) }
        finally { canvas.restoreToCount(salvo) }
    }
}

private fun criarPin(context: Context, numero: Int, cor: Int): BitmapDrawable {
    val densidade = context.resources.displayMetrics.density
    val tamanho = (36 * densidade).toInt()
    val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centro = tamanho / 2f
    canvas.drawCircle(centro, centro, centro, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor })
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16 * densidade
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(numero.toString(), centro, centro - (texto.ascent() + texto.descent()) / 2, texto)
    return BitmapDrawable(context.resources, bitmap)
}

private var configurado = false
private fun configurarMapa(context: Context) {
    if (configurado) return
    val app = context.applicationContext
    Configuration.getInstance().apply {
        load(app, app.getSharedPreferences("mapa", Context.MODE_PRIVATE))
        userAgentValue = "com.monkeymart.rotaspoc/1.0"
        osmdroidBasePath = File(app.cacheDir, "osmdroid").apply { mkdirs() }
        osmdroidTileCache = File(osmdroidBasePath, "tiles").apply { mkdirs() }
    }
    configurado = true
}
