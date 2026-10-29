package com.example.reversapoc

import android.content.Context
import android.graphics.Canvas
import android.view.ViewOutlineProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun Mapa(ponto: Coordenadas?, titulo: String?, onToque: (Coordenadas) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tocar by rememberUpdatedState(onToque)
    val mapa = remember(context) { criarMapa(context) { tocar(it) } }
    val marcador = remember(mapa) {
        Marker(mapa).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_pin_monkey)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(mapa, lifecycle) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapa.onPause()
            mapa.onDetach()
        }
    }
    AndroidView(factory = { mapa }, modifier = modifier.clipToBounds(), update = { view ->
        if (ponto == null) {
            marcador.closeInfoWindow()
            view.overlays.remove(marcador)
        } else {
            val geo = GeoPoint(ponto.latitude, ponto.longitude)
            if (marcador.position != geo || marcador.title != titulo) {
                marcador.position = geo
                marcador.title = titulo
                if (marcador !in view.overlays) view.overlays.add(marcador)
                view.controller.setCenter(geo)
                if (titulo != null) marcador.showInfoWindow() else marcador.closeInfoWindow()
            }
        }
        view.invalidate()
    })
}

private fun criarMapa(context: Context, onToque: (Coordenadas) -> Unit): MapView {
    val app = context.applicationContext
    Configuration.getInstance().load(app, PreferenceManager.getDefaultSharedPreferences(app))
    Configuration.getInstance().userAgentValue = "MonkeyMart/1.0"
    return MapaLimitado(context).apply {
        setDestroyMode(false)
        setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors"))
        setMultiTouchControls(true)
        controller.setZoom(18.0)
        // Centro inicial: Praça da Sé, São Paulo.
        controller.setCenter(GeoPoint(-23.5505, -46.6333))
        overlays.add(CopyrightOverlay(context))
        overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onToque(Coordenadas(p.latitude, p.longitude))
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        }))
    }
}

private class MapaLimitado(context: Context) : MapView(context) {
    init {
        clipChildren = true
        clipToPadding = true
        outlineProvider = ViewOutlineProvider.BOUNDS
        clipToOutline = true
    }

    override fun draw(canvas: Canvas) {
        val salvo = canvas.save()
        try {
            canvas.clipRect(0, 0, width, height)
            super.draw(canvas)
        } finally { canvas.restoreToCount(salvo) }
    }
}
