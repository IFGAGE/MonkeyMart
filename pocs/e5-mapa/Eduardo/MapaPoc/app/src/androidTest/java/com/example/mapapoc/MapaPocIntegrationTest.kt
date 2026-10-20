package com.example.mapapoc

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@RunWith(AndroidJUnit4::class)
class MapaPocIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun buscaAtualizaDestinoSemTrocarTituloEnquantoDigita() {
        val primeiro = "Praca da Se, Sao Paulo, SP, Brasil"
        compose.onNode(hasSetTextAction()).performTextReplacement(primeiro)
        compose.onNodeWithText("Buscar").performClick()
        compose.waitUntil(30_000) { mapa() != null }
        val segundo = "Praca dos Tres Poderes, Brasilia, DF, Brasil"
        compose.onNode(hasSetTextAction()).performTextReplacement(segundo)
        compose.runOnIdle {
            val map = encontrarMapa(compose.activity.window.decorView)!!
            assertEquals(primeiro, map.overlays.filterIsInstance<Marker>().single().title)
            assertTrue(map.overlays.any { it is CopyrightOverlay })
            assertEquals("MonkeyMartEntrega5POC/1.0", Configuration.getInstance().userAgentValue)
        }
        compose.onNodeWithText("Buscar").performClick()
        compose.waitUntil(30_000) { mapa()?.overlays?.filterIsInstance<Marker>()?.firstOrNull()?.title == segundo }
        compose.runOnIdle {
            val marker = encontrarMapa(compose.activity.window.decorView)!!.overlays.filterIsInstance<Marker>().single()
            assertTrue(marker.position.latitude in -15.85..-15.75)
            assertTrue(marker.position.longitude in -47.95..-47.80)
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        Thread.sleep(10_000) // Aguarda os tiles da visualização interativa antes da evidência.
        val imagem = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir, "mapa-poc.png").outputStream().use {
            imagem.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        imagem.recycle()
    }
    private fun mapa(): MapView? {
        var result: MapView? = null
        compose.runOnUiThread { result = encontrarMapa(compose.activity.window.decorView) }
        return result
    }
    private fun encontrarMapa(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) encontrarMapa(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
