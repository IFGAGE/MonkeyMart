package com.example.marketplace.ui.screens.entregador

import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.proximidade.pontoAoNorte
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizacaoDaParadaTest {
    private val coleta = Coordenadas(-21.784, -48.2107)
    private val entrega = pontoAoNorte(coleta, 600.0)

    @Test fun liberaApenasAParadaProximaDaPosicaoRecebida() {
        assertTrue(distanciaAteParada(coleta, coleta, 10f, 0L).dentroDoRaioDaParada())
        assertFalse(distanciaAteParada(coleta, entrega, 10f, 0L).dentroDoRaioDaParada())
        assertTrue(distanciaAteParada(pontoAoNorte(entrega, 199.0), entrega, 10f, 0L).dentroDoRaioDaParada())
        assertFalse(distanciaAteParada(pontoAoNorte(entrega, 201.0), entrega, 10f, 0L).dentroDoRaioDaParada())
    }

    @Test fun mantemOsChecksBloqueadosSemUmaPosicaoValidaERecente() {
        assertFalse(distanciaAteParada(coleta, coleta, 10f, 30_000L).dentroDoRaioDaParada())
        assertFalse(distanciaAteParada(coleta, coleta, 101f, 0L).dentroDoRaioDaParada())
        assertFalse(distanciaAteParada(coleta, coleta, null, 0L).dentroDoRaioDaParada())
    }
}
