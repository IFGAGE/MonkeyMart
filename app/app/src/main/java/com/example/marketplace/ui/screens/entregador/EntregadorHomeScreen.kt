package com.example.marketplace.ui.screens.entregador

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.marketplace.R

data class PedidoTemp(
    val id: Int,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregadorHomeScreen(
    pedidos: List<PedidoTemp>,
    temRota: Boolean,
    gerandoRota: Boolean,
    erroRota: String?,
    onAddVeiculoClick: () -> Unit,
    onProfileClick: () -> Unit,
    onGerarRota: (List<PedidoTemp>) -> Unit,
    onContinuarRota: () -> Unit,
    onPermissaoNegada: () -> Unit,
    onVerMapaClick: (Int, String) -> Unit
) {
    val context = LocalContext.current
    var selecionados by remember { mutableStateOf(emptySet<Int>()) }
    val pedidosSelecionados = pedidos.filter { it.id in selecionados }
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes.values.any { it }) onGerarRota(pedidosSelecionados)
        else onPermissaoNegada()
    }

    LaunchedEffect(pedidos) {
        selecionados = selecionados.intersect(pedidos.map { it.id }.toSet())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(
                            if (isSystemInDarkTheme()) R.drawable.mm_branco else R.drawable.mm_preto
                        ),
                        contentDescription = null,
                        modifier = Modifier.height(40.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAddVeiculoClick) {
                        Icon(Icons.Default.LocalShipping, contentDescription = "Cadastrar veículo")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Pedidos Disponíveis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (temRota) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Você possui uma rota em andamento.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onContinuarRota, modifier = Modifier.fillMaxWidth()) {
                            Text("Continuar rota")
                        }
                    }
                }
            } else if (pedidos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum pedido pendente no momento.", color = Color.Gray)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selecionados.size == pedidos.size,
                        onCheckedChange = { marcarTodos ->
                            selecionados = if (marcarTodos) pedidos.map { it.id }.toSet() else emptySet()
                        }
                    )
                    Text("Selecionar todos")
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pedidos, key = { it.id }) { pedido ->
                        PedidoSelecionavel(
                            pedido = pedido,
                            selecionado = pedido.id in selecionados,
                            onSelecionar = { marcado ->
                                selecionados = if (marcado) selecionados + pedido.id else selecionados - pedido.id
                            },
                            onVerMapa = { onVerMapaClick(pedido.id, pedido.enderecoEntrega) }
                        )
                    }
                }

                erroRota?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = {
                        val temPermissao = listOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                        if (temPermissao) onGerarRota(pedidosSelecionados)
                        else permissaoLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    enabled = selecionados.isNotEmpty() && !gerandoRota,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    if (gerandoRota) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar rota")
                    }
                }
            }
        }
    }
}

@Composable
private fun PedidoSelecionavel(
    pedido: PedidoTemp,
    selecionado: Boolean,
    onSelecionar: (Boolean) -> Unit,
    onVerMapa: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selecionado, onCheckedChange = onSelecionar)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedido #${pedido.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("R$ ${pedido.valorTotal}", color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(pedido.enderecoEntrega, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onVerMapa, modifier = Modifier.align(Alignment.End)) {
                Text("Visualizar no mapa")
            }
        }
    }
}
