package com.example.marketplace.ui.mapa

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// Cada AndroidView é dono da própria View. Uma remoção temporária da lista
// pausa o mapa; somente onRelease libera os recursos definitivamente.
internal class MapaRotaView(context: Context) : MapaLimitado(context) {
    var rotaDesenhada: RotaGerada? = null
    var enquadrado = false
    private var ciclo: Lifecycle? = null
    private var retomado = false
    private var emReuso = false
    private var liberado = false
    private val observer = LifecycleEventObserver { _, _ -> atualizarAtividade() }

    fun acompanharCiclo(lifecycle: Lifecycle) {
        if (ciclo !== lifecycle) {
            ciclo?.removeObserver(observer)
            ciclo = lifecycle
            lifecycle.addObserver(observer)
        }
        emReuso = false
        atualizarAtividade()
    }

    fun prepararReuso() {
        emReuso = true
        atualizarAtividade()
    }

    private fun atualizarAtividade() {
        val deveRetomar = !liberado && !emReuso && isAttachedToWindow &&
            ciclo?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        if (deveRetomar == retomado) return
        retomado = deveRetomar
        if (retomado) onResume() else onPause()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        atualizarAtividade()
    }

    override fun onDetachedFromWindow() {
        if (retomado) { retomado = false; onPause() }
        super.onDetachedFromWindow()
    }

    fun liberar() {
        if (liberado) return
        liberado = true
        ciclo?.removeObserver(observer)
        ciclo = null
        atualizarAtividade()
        onDetach()
    }
}
