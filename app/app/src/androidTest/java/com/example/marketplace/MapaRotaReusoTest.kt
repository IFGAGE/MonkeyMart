package com.example.marketplace

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.marketplace.ui.mapa.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@RunWith(AndroidJUnit4::class)
class MapaRotaReusoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mapaVoltaAparecerAposExpandirCardsERolarMantendoCamera() {
        lateinit var raiz: View
        val origem = Coordenadas(-21.78, -48.20)
        val destino = Coordenadas(-21.79, -48.21)
        val rota = RotaGerada(origem, listOf(ParadaRota(1, "Entrega", destino)), listOf(origem, destino))
        compose.setContent {
            raiz = LocalView.current.rootView
            MaterialTheme {
                val estado = lembrarEstadoMapaRota()
                LazyColumn(Modifier.fillMaxSize().testTag("lista")) {
                    item(key = "mapa") { MapaRota(rota, Modifier.fillMaxWidth().height(200.dp), estado) }
                    items((0..40).toList(), key = { it }) { numero ->
                        var aberto by remember { mutableStateOf(false) }
                        Column {
                            TextButton(onClick = { aberto = !aberto }) { Text("Pedido $numero") }
                            if (aberto) Text("Detalhes do pedido", Modifier.height(160.dp))
                        }
                    }
                }
            }
        }
        fun mapaVisivel(): MapView {
            fun localizar(view: View): MapView? {
                if (view is MapView && view.isShown && view.getGlobalVisibleRect(Rect())) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) localizar(view.getChildAt(i))?.let { return it }
                return null
            }
            return checkNotNull(localizar(raiz)) { "O espaço do mapa ficou sem a View nativa." }
        }
        compose.runOnIdle {
            mapaVisivel().apply {
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(origem.latitude, origem.longitude))
            }
        }
        repeat(4) {
            compose.onNodeWithText("Pedido 0").performClick()
            compose.onNodeWithTag("lista").performScrollToIndex(35)
            compose.onNodeWithTag("lista").performScrollToIndex(0)
            compose.waitForIdle()
            compose.runOnIdle {
                val mapa = mapaVisivel()
                assertNotNull(mapa.parent)
                assertTrue(mapa.overlays.any { it is Polyline })
                assertEquals(14.0, mapa.zoomLevelDouble, 0.01)
            }
        }
    }
}
