package com.example.marketplace.ui.screens.entregador

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.marketplace.R
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.mapa.LocalPedido
import com.example.marketplace.ui.mapa.TipoParada
import com.example.marketplace.ui.mapa.localNoMapa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregadorHomeScreen(
    pedidos: List<PedidoTemp>,
    rota: RotaEntity?,
    podeCancelarRota: Boolean,
    temVeiculo: Boolean,
    gerandoRota: Boolean,
    erroRota: String?,
    onAddVeiculoClick: () -> Unit,
    onProfileClick: () -> Unit,
    onGerarRota: (List<PedidoTemp>) -> Unit,
    onContinuarRota: () -> Unit,
    onCancelarRota: (RotaEntity) -> Unit,
    onPermissaoNegada: () -> Unit,
    onVerMapaClick: (LocalPedido) -> Unit
) {
    val context = LocalContext.current
    var cancelar by remember { mutableStateOf<RotaEntity?>(null) }
    cancelar?.let { selecionada ->
        ConfirmarCancelamentoRota(gerandoRota, { cancelar = null }, {
            cancelar = null
            onCancelarRota(selecionada)
        }, entregasIniciadas = selecionada.estado == "INICIADA")
    }
    var selecionados by remember { mutableStateOf(emptySet<Int>()) }
    val pedidosSelecionados = pedidos.filter { it.id in selecionados }
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes.values.any { it } && temVeiculo) onGerarRota(pedidosSelecionados)
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
            if (rota == null) Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pedidos disponíveis", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                if (pedidos.isNotEmpty()) Row(
                    modifier = Modifier.toggleable(
                        value = selecionados.size == pedidos.size,
                        role = Role.Checkbox,
                        onValueChange = { marcarTodos ->
                            selecionados = if (marcarTodos) pedidos.map { it.id }.toSet() else emptySet()
                        }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selecionados.size == pedidos.size, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Selecionar todos", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!temVeiculo && rota == null) {
                Text("Cadastre um veículo para gerar uma rota.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onAddVeiculoClick) { Text("Cadastrar veículo") }
            }

            if (rota != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(when (rota.estado) {
                            "INICIADA" -> "Entregas em andamento"
                            "RESERVANDO" -> "O início das entregas está pendente"
                            else -> "A rota já foi planejada"
                        }, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(when (rota.estado) {
                            "INICIADA" -> "Acompanhe as paradas e confirme cada coleta e entrega."
                            "RESERVANDO" -> "Abra a rota para tentar iniciar novamente ou cancele o planejamento."
                            else -> "Confira as paradas e inicie as entregas quando estiver pronto."
                        }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onContinuarRota, enabled = !gerandoRota, modifier = Modifier.fillMaxWidth()) {
                            Text(if (rota.estado == "INICIADA") "Ver entregas" else "Ver rota planejada")
                        }
                        if (podeCancelarRota) TextButton(onClick = { cancelar = rota }, enabled = !gerandoRota,
                            modifier = Modifier.fillMaxWidth()) { Text(if (rota.estado == "INICIADA") "Cancelar entregas" else "Cancelar rota") }
                        else if (rota.estado == "INICIADA") Text("Após uma coleta, conclua as entregas para encerrar.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        erroRota?.let { Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    }
                }
            } else if (pedidos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum pedido pendente no momento.", color = Color.Gray)
                }
            } else {
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
                            onVerColeta = { onVerMapaClick(pedido.localNoMapa(TipoParada.COLETA)) },
                            onVerEntrega = { onVerMapaClick(pedido.localNoMapa(TipoParada.ENTREGA)) }
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
                    enabled = selecionados.isNotEmpty() && !gerandoRota && temVeiculo,
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
    onVerColeta: () -> Unit,
    onVerEntrega: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selecionado, onCheckedChange = onSelecionar)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("Pedido #${pedido.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("R$ ${pedido.valorTotal}", color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onVerColeta, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Coleta")
                    }
                    OutlinedButton(onClick = onVerEntrega, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Entrega")
                    }
                }
            }
        }
    }
}
