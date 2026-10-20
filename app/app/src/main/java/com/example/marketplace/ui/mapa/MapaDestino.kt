package com.example.marketplace.ui.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import com.example.marketplace.R

@Composable
fun MapaDestino(resultado: EstadoBuscaEndereco.Encontrado, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val map = remember(context) {
        val appContext = context.applicationContext
        Configuration.getInstance().load(appContext, PreferenceManager.getDefaultSharedPreferences(appContext))
        Configuration.getInstance().userAgentValue = "MonkeyMart/1.0"
        MapView(context).apply {
            setDestroyMode(false)
            setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
                arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors"))
            setMultiTouchControls(true)
            controller.setZoom(18.0)
            overlays.add(CopyrightOverlay(context))
        }
    }
    val marker = remember(map) {
        Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_pin_monkey)
            map.overlays.add(this)
        }
    }
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
        modifier = modifier
    )
}
