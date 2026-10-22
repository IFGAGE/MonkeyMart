package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.marketplace.ui.theme.ColoredLinks

import com.example.marketplace.ui.mapa.*
import com.example.marketplace.data.entity.EnderecoValidado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEntregaScreen(
    pedidoId: Int,
    endereco: String,
    onVoltar: () -> Unit,
    tipo: TipoParada = TipoParada.ENTREGA,
    coordenadas: Coordenadas? = null
) {
    val context = LocalContext.current.applicationContext
    val ponto = coordenadas?.takeIf { EnderecoValidado(endereco, it.latitude, it.longitude).valido() }
    var estado by remember(endereco, ponto) { mutableStateOf<EstadoBuscaEndereco>(
        ponto?.let { EstadoBuscaEndereco.Encontrado(endereco, it) } ?: EstadoBuscaEndereco.Carregando) }
    var tentativa by remember(endereco) { mutableStateOf(0) }

    LaunchedEffect(endereco, ponto, tentativa) {
        if (ponto != null) estado = EstadoBuscaEndereco.Encontrado(endereco, ponto)
        else {
            estado = EstadoBuscaEndereco.Carregando
            estado = buscarEndereco(endereco, { localizarEndereco(context, it) })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${if (tipo == TipoParada.COLETA) "Coleta" else "Entrega"} do pedido #$pedidoId") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tipo == TipoParada.COLETA) Icons.Default.Storefront else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ColoredLinks
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = endereco,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (val atual = estado) {
                    is EstadoBuscaEndereco.Encontrado -> MapaDestino(atual, Modifier.fillMaxSize())
                    is EstadoBuscaEndereco.Erro -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(atual.mensagem, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { tentativa++ }) { Text("Tentar novamente") }
                    }
                    else -> CircularProgressIndicator(color = ColoredLinks)
                }
            }
        }
    }
}
