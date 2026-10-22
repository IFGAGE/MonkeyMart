package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marketplace.data.entity.EnderecoColeta
import com.example.marketplace.data.entity.EnderecoValidado
import com.example.marketplace.ui.mapa.CampoEnderecoMapa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnderecoColetaScreen(enderecoInicial: EnderecoColeta?, onSalvar: suspend (EnderecoColeta) -> Unit,
    onVoltar: () -> Unit) {
    var confirmado by remember { mutableStateOf<EnderecoValidado?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Endereço de coleta") }, navigationIcon = {
            IconButton(onClick = onVoltar, enabled = !salvando) { Icon(Icons.Default.ArrowBack, "Voltar") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Informe onde seus produtos serão coletados e confira o local no mapa.")
            CampoEnderecoMapa(enderecoInicial = enderecoInicial?.texto().orEmpty(),
                confirmadoInicial = enderecoInicial?.validado(), enabled = !salvando,
                onValidado = { confirmado = it; erro = null })
            erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val endereco = confirmado ?: return@Button
                salvando = true
                scope.launch {
                    try {
                        onSalvar(EnderecoColeta(endereco = endereco.endereco,
                            latitude = endereco.latitude, longitude = endereco.longitude))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { erro = "Não foi possível salvar o endereço. Tente novamente." }
                    finally { salvando = false }
                }
            }, enabled = confirmado?.valido() == true && !salvando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (salvando) "Salvando..." else "Salvar endereço de coleta")
            }
        }
    }
}
