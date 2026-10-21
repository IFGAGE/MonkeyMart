package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketplace.data.entity.EnderecoValidado
import com.example.marketplace.ui.mapa.CampoEnderecoMapa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizarCompraScreen(total: String, quantidadeVendedores: Int, salvando: Boolean,
    podeFinalizar: Boolean, erro: String?, onEndereco: (EnderecoValidado?) -> Unit,
    onConfirmar: () -> Unit, onVoltar: () -> Unit) {
    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(title = { Text("Finalizar compra") }, navigationIcon = {
            IconButton(onClick = onVoltar, enabled = !salvando) { Icon(Icons.Default.ArrowBack, "Voltar") }
        })
    }, bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text("R$ $total", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(onClick = onConfirmar, enabled = podeFinalizar && !salvando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (salvando) "Concluindo..." else "Confirmar compra")
                }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CampoEnderecoMapa(enabled = !salvando, compacto = true, rotulo = "Endereço de entrega", onValidado = onEndereco)
            if (quantidadeVendedores > 1) {
                Text("Sua compra será dividida em $quantidadeVendedores pedidos, um por vendedor.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
