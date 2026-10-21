package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaNegocianteScreen(onEndereco: () -> Unit, onNovoProduto: () -> Unit,
    onMeusProdutos: () -> Unit, onPedidosClientes: () -> Unit, onVoltar: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Área do negociante") }, navigationIcon = {
            IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, "Voltar") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            listOf(
                Triple("Pedidos recebidos", Icons.Default.ListAlt, onPedidosClientes),
                Triple("Meus produtos", Icons.Default.Inventory2, onMeusProdutos),
                Triple("Cadastrar produto", Icons.Default.Add, onNovoProduto)
            ).forEach { (titulo, icone, acao) ->
                ListItem(
                    headlineContent = { Text(titulo, fontSize = 18.sp) },
                    leadingContent = { Icon(icone, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = acao)
                )
                HorizontalDivider()
            }
            Spacer(Modifier.height(24.dp))
            Text("Configurações", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ListItem(headlineContent = { Text("Endereço de coleta", fontSize = 18.sp) },
                leadingContent = { Icon(Icons.Default.LocationOn, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(onClick = onEndereco))
            HorizontalDivider()
        }
    }
}
