package com.example.mapapoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.mapapoc.mapa.*
import com.example.mapapoc.ui.theme.MapaPocTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapaPocTheme {
                var enderecoInput by remember { mutableStateOf("") }
                var estado by remember { mutableStateOf<EstadoBuscaEndereco>(EstadoBuscaEndereco.Inicial) }
                val scope = rememberCoroutineScope()
                val focus = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                Scaffold(topBar = { TopAppBar(title = { Text("Validação: Geocoding + OSM") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = enderecoInput,
                                onValueChange = { enderecoInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Digite o endereço") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val consulta = enderecoInput
                                    focus.clearFocus()
                                    keyboard?.hide()
                                    estado = EstadoBuscaEndereco.Carregando
                                    scope.launch {
                                        estado = buscarEndereco(consulta, { localizarEndereco(applicationContext, it) })
                                    }
                                },
                                enabled = estado != EstadoBuscaEndereco.Carregando && enderecoInput.isNotBlank()
                            ) { Text("Buscar") }
                        }
                        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) {
                            when (val atual = estado) {
                                EstadoBuscaEndereco.Inicial -> Text("Insira um endereço para visualizar no mapa.")
                                EstadoBuscaEndereco.Carregando -> CircularProgressIndicator()
                                is EstadoBuscaEndereco.Encontrado -> MapaDestino(atual, Modifier.fillMaxSize())
                                is EstadoBuscaEndereco.Erro -> Text(atual.mensagem, color = Color.Red, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
