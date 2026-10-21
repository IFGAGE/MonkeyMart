package com.example.marketplace.ui.mapa

import android.content.Context
import android.graphics.Canvas
import android.view.ViewOutlineProvider
import org.osmdroid.views.MapView

// O AndroidView não recorta o desenho nativo por padrão. O recorte do Canvas
// também cobre as transformações de zoom e janelas dos marcadores do osmdroid.
internal open class MapaLimitado(context: Context) : MapView(context) {
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
