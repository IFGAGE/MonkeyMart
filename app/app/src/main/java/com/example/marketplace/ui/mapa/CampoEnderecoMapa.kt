package com.example.marketplace.ui.mapa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.marketplace.data.entity.EnderecoValidado

@Composable
fun CampoEnderecoMapa(enderecoInicial: String = "", confirmadoInicial: EnderecoValidado? = null,
    enabled: Boolean = true, compacto: Boolean = false, rotulo: String = "Endereço completo",
    onValidado: (EnderecoValidado?) -> Unit) {
    val context = LocalContext.current
    var texto by rememberSaveable { mutableStateOf(enderecoInicial) }
    // Endereços salvos antes da restrição de país precisam passar novamente
    // pelo serviço; coordenadas sozinhas não identificam o país com segurança.
    var consulta by remember {
        mutableStateOf(confirmadoInicial?.takeIf { it.valido() && it.endereco == texto.trim() }?.endereco)
    }
    var tentativa by remember { mutableStateOf(0) }
    var estado by remember {
        mutableStateOf<EstadoBuscaEndereco>(EstadoBuscaEndereco.Inicial)
    }
    val notificar by rememberUpdatedState(onValidado)
    LaunchedEffect(Unit) {
        notificar((estado as? EstadoBuscaEndereco.Encontrado)?.let {
            EnderecoValidado(it.endereco, it.coordenadas.latitude, it.coordenadas.longitude)
        })
    }
    LaunchedEffect(consulta, tentativa, texto) {
        val endereco = consulta ?: return@LaunchedEffect
        if (endereco != texto.trim()) return@LaunchedEffect
        estado = EstadoBuscaEndereco.Carregando
        notificar(null)
        val resultado = buscarEndereco(endereco, { localizarEndereco(context, it) })
        estado = resultado
        notificar((resultado as? EstadoBuscaEndereco.Encontrado)?.let {
            EnderecoValidado(it.endereco, it.coordenadas.latitude, it.coordenadas.longitude).takeIf { ponto -> ponto.valido() }
        })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = texto, onValueChange = {
            texto = it
            consulta = null
            estado = EstadoBuscaEndereco.Inicial
            notificar(null)
        }, label = { Text(rotulo) },
            placeholder = if (compacto) { { Text("Rua, número, cidade e estado") } } else null,
            supportingText = { Text(if (compacto) "Somente endereços no Brasil." else "Inclua rua, número, bairro, cidade e estado. Somente Brasil.") },
            enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            notificar(null)
            estado = EstadoBuscaEndereco.Carregando
            consulta = texto.trim()
            tentativa++
        }, enabled = enabled && texto.isNotBlank() && estado !is EstadoBuscaEndereco.Carregando,
            modifier = if (compacto) Modifier.fillMaxWidth() else Modifier) {
            Text("Buscar no mapa")
        }
        when (val resultado = estado) {
            is EstadoBuscaEndereco.Carregando -> CircularProgressIndicator(Modifier.size(28.dp))
            is EstadoBuscaEndereco.Erro -> Text(resultado.mensagem, color = MaterialTheme.colorScheme.error)
            is EstadoBuscaEndereco.Encontrado -> {
                MapaDestino(resultado, Modifier.fillMaxWidth().height(if (compacto) 190.dp else 220.dp))
                Text(if (compacto) "Confira o local de entrega no mapa." else "Confira se o ponto no mapa corresponde ao endereço informado.")
            }
            else -> if (!compacto) Text("Busque o endereço no mapa para continuar.")
        }
    }
}
