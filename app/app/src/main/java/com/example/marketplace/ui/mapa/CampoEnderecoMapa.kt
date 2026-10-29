package com.example.marketplace.ui.mapa

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.marketplace.data.entity.EnderecoValidado
import kotlinx.coroutines.CancellationException
import java.io.IOException


private class PedidoPonto(val ponto: Coordenadas?)

private val permissoesLocalizacao = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun CampoEnderecoMapa(enderecoInicial: String = "", confirmadoInicial: EnderecoValidado? = null,
    enabled: Boolean = true, compacto: Boolean = false, rotulo: String = "Endereço completo",
    onValidado: (EnderecoValidado?) -> Unit) {
    val context = LocalContext.current
    var texto by rememberSaveable { mutableStateOf(enderecoInicial) }
   
    var consulta by remember {
        mutableStateOf(confirmadoInicial?.takeIf { it.valido() && it.endereco == texto.trim() }?.endereco)
    }
    var tentativa by remember { mutableStateOf(0) }
    var estado by remember {
        mutableStateOf<EstadoBuscaEndereco>(EstadoBuscaEndereco.Inicial)
    }
    var pedidoPonto by remember { mutableStateOf<PedidoPonto?>(null) }
    var marcador by remember { mutableStateOf<EstadoBuscaEndereco.Encontrado?>(null) }
    var enderecoDoMapa by remember { mutableStateOf(false) }
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
        marcador = resultado as? EstadoBuscaEndereco.Encontrado
        notificar((resultado as? EstadoBuscaEndereco.Encontrado)?.let {
            EnderecoValidado(it.endereco, it.coordenadas.latitude, it.coordenadas.longitude).takeIf { ponto -> ponto.valido() }
        })
    }
    LaunchedEffect(pedidoPonto) {
        val pedido = pedidoPonto ?: return@LaunchedEffect
        estado = EstadoBuscaEndereco.Carregando
        notificar(null)
        val ponto = pedido.ponto ?: try {
            localizarAtual(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            estado = EstadoBuscaEndereco.Erro((e as? IOException)?.message ?: "Não foi possível obter sua localização.")
            return@LaunchedEffect
        }
        marcador = EstadoBuscaEndereco.Encontrado(marcador?.endereco ?: "Localização atual", ponto)
        val resultado = buscarEnderecoPorPonto(ponto, { enderecoDoPonto(context, it) })
        if (resultado is EstadoBuscaEndereco.Encontrado) {
            texto = resultado.endereco
            marcador = resultado
            enderecoDoMapa = true
        }
        estado = resultado
        notificar((resultado as? EstadoBuscaEndereco.Encontrado)?.let {
            EnderecoValidado(it.endereco, it.coordenadas.latitude, it.coordenadas.longitude).takeIf { ponto -> ponto.valido() }
        })
    }
    fun buscarPorPonto(ponto: Coordenadas?) {
        consulta = null
        pedidoPonto = PedidoPonto(ponto)
    }
    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { concedidas ->
        if (concedidas.values.any { it }) buscarPorPonto(null)
        else estado = EstadoBuscaEndereco.Erro("Permita o acesso à localização ou digite o endereço.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = texto, onValueChange = {
            texto = it
            consulta = null
            pedidoPonto = null
            marcador = null
            enderecoDoMapa = false
            estado = EstadoBuscaEndereco.Inicial
            notificar(null)
        }, label = { Text(rotulo) },
            placeholder = if (compacto) { { Text("Rua, número, cidade e estado") } } else null,
            supportingText = { Text(if (compacto) "Somente endereços no Brasil." else "Inclua rua, número, bairro, cidade e estado. Somente Brasil.") },
            enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            notificar(null)
            pedidoPonto = null
            marcador = null
            enderecoDoMapa = false
            estado = EstadoBuscaEndereco.Carregando
            consulta = texto.trim()
            tentativa++
        }, enabled = enabled && texto.isNotBlank() && estado !is EstadoBuscaEndereco.Carregando,
            modifier = if (compacto) Modifier.fillMaxWidth() else Modifier) {
            Text("Buscar no mapa")
        }
        TextButton(onClick = {
            val permitido = permissoesLocalizacao.any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (permitido) buscarPorPonto(null) else permissao.launch(permissoesLocalizacao)
        }, enabled = enabled && estado !is EstadoBuscaEndereco.Carregando) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Usar minha localização")
        }
        when (val resultado = estado) {
            is EstadoBuscaEndereco.Carregando -> CircularProgressIndicator(Modifier.size(28.dp))
            is EstadoBuscaEndereco.Erro -> Text(resultado.mensagem, color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
        marcador?.let { ponto ->
            MapaDestino(ponto, Modifier.fillMaxWidth().height(if (compacto) 190.dp else 220.dp),
                onToque = if (enabled) { toque -> buscarPorPonto(toque) } else null)
        }
        when (estado) {
            is EstadoBuscaEndereco.Encontrado -> Text(when {
                enderecoDoMapa -> "Endereço aproximado do ponto marcado. Confira o número; toque no mapa para ajustar."
                compacto -> "Confira o local de entrega no mapa. Toque no mapa para ajustar."
                else -> "Confira se o ponto no mapa corresponde ao endereço informado. Toque no mapa para ajustar."
            })
            is EstadoBuscaEndereco.Inicial -> if (!compacto) Text("Busque o endereço no mapa para continuar.")
            else -> Unit
        }
    }
}
