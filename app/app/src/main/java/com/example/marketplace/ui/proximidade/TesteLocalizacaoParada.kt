package com.example.marketplace.ui.proximidade

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.marketplace.ui.mapa.Coordenadas
import java.util.Locale

@Composable
internal fun TesteLocalizacaoParada(destino: Coordenadas) {
    val context = LocalContext.current
    if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mostrar by remember { mutableStateOf(false) }
    var distancia by remember(destino) { mutableStateOf("Aguardando posição do Android…") }
    if (mostrar) {
        // Apenas exibe a posição recebida; não registra aproximação nem altera o GPS.
        LaunchedEffect(destino, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observarLocalizacao(context).collect { ponto ->
                    val metros = FloatArray(1)
                    android.location.Location.distanceBetween(ponto.latitude, ponto.longitude,
                        destino.latitude, destino.longitude, metros)
                    distancia = String.format(Locale.forLanguageTag("pt-BR"), "Distância recebida: %.0f m", metros[0])
                }
            }
        }
        val fora = pontoAoNorte(destino, MonitorGeofence.RAIO * 2.0)
        val dentro = pontoAoNorte(destino, MonitorGeofence.RAIO / 2.0)
        AlertDialog(onDismissRequest = { mostrar = false },
            title = { Text("Teste de localização") },
            text = {
                SelectionContainer {
                    Column {
                        Text("Raio: ${MonitorGeofence.RAIO.toInt()} m")
                        Text("Destino: ${formatar(destino)}")
                        Text("Fora (~400 m): ${formatar(fora)}")
                        Text("Dentro (~100 m): ${formatar(dentro)}")
                        Text(distancia)
                        Text("Use estas coordenadas no Mock Locations.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrar = false }) { Text("Fechar") } })
    }
    TextButton(onClick = { mostrar = true }) { Text("Testar localização") }
}

private fun formatar(ponto: Coordenadas) = String.format(Locale.US, "%.6f, %.6f", ponto.latitude, ponto.longitude)
