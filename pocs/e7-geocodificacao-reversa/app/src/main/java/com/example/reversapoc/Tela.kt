package com.example.reversapoc

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

sealed class Estado {
    object Inicial : Estado()
    object Carregando : Estado()
    data class Encontrado(val endereco: EnderecoEncontrado, val ponto: Coordenadas) : Estado()
    data class Erro(val mensagem: String) : Estado()
}

@Composable
fun Tela() {
    val context = LocalContext.current
    val servicos = remember { Servicos(context) }
    val scope = rememberCoroutineScope()
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var ponto by remember { mutableStateOf<Coordenadas?>(null) }
    var estado by remember { mutableStateOf<Estado>(Estado.Inicial) }
    var busca by remember { mutableStateOf<Job?>(null) }

    fun consultar(obterPonto: suspend () -> Coordenadas) {
        busca?.cancel()
        estado = Estado.Carregando
        busca = scope.launch {
            estado = try {
                val alvo = obterPonto()
                ponto = alvo
                latitude = String.format(Locale.US, "%.6f", alvo.latitude)
                longitude = String.format(Locale.US, "%.6f", alvo.longitude)
                Estado.Encontrado(servicos.enderecoDoPonto(alvo), alvo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Estado.Erro(e.message ?: "Falha ao consultar o endereço.")
            }
        }
    }

    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { concedida -> concedida }) consultar { servicos.localizarAtual() }
        else estado = Estado.Erro("Permissão de localização negada.")
    }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Geocodificação reversa", style = MaterialTheme.typography.headlineSmall)
            Text("Toque no mapa, use sua localização ou digite coordenadas para descobrir o endereço.",
                style = MaterialTheme.typography.bodyMedium)

            Mapa(ponto, titulo = (estado as? Estado.Encontrado)?.endereco?.texto,
                onToque = { toque -> consultar { toque } }, modifier = Modifier.fillMaxWidth().height(260.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val digitado = Coordenadas(lerCoordenada(latitude) ?: Double.NaN, lerCoordenada(longitude) ?: Double.NaN)
                    if (digitado.validas()) consultar { digitado }
                    else estado = Estado.Erro("Informe latitude (-90 a 90) e longitude (-180 a 180).")
                }, modifier = Modifier.weight(1f)) { Text("Buscar endereço") }
                OutlinedButton(onClick = {
                    permissao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.weight(1f)) { Text("Minha localização") }
            }

            when (val atual = estado) {
                Estado.Inicial -> Unit
                Estado.Carregando -> CircularProgressIndicator(Modifier.size(28.dp))
                is Estado.Erro -> Text(atual.mensagem, color = MaterialTheme.colorScheme.error)
                is Estado.Encontrado -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Endereço encontrado", style = MaterialTheme.typography.labelLarge)
                        Text(atual.endereco.texto, style = MaterialTheme.typography.bodyLarge)
                        if (!atual.endereco.noBrasil) Text(
                            "Fora do Brasil: o app MonkeyMart aceita apenas endereços no Brasil.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text("Aproximado: confira o número no local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
