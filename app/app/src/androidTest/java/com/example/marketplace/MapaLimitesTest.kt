package com.example.marketplace

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.ui.mapa.MapaLimitado
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

@RunWith(AndroidJUnit4::class)
class MapaLimitesTest {
    @Test fun desenhoDoMapaNaoInvadeOsElementosVizinhos() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val mapa = MapaLimitado(instrumentation.targetContext)
            try {
                mapa.overlays.add(object : Overlay() {
                    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                        // Simula conteúdo ampliado pelo zoom, além dos limites do mapa.
                        canvas.drawRect(-1000f, -1000f, 1000f, 1000f, Paint().apply { color = Color.RED })
                    }
                })
                val medida = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
                mapa.measure(medida, medida)
                mapa.layout(0, 0, 100, 100)
                val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.translate(100f, 100f)
                mapa.draw(canvas)
                assertEquals(Color.WHITE, bitmap.getPixel(50, 150))
                assertEquals(Color.WHITE, bitmap.getPixel(250, 150))
                assertEquals(Color.WHITE, bitmap.getPixel(150, 50))
                assertEquals(Color.WHITE, bitmap.getPixel(150, 250))
                assertEquals(Color.RED, bitmap.getPixel(150, 150))
                bitmap.recycle()
            } finally { mapa.onDetach() }
        }
    }
}
