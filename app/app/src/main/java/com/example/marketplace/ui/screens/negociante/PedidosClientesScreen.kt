package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.descricaoStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosClientesScreen(pedidos: List<PedidoEntity>, onPronto: suspend (Int) -> Unit, onVoltar: () -> Unit) {
    var processando by remember { mutableStateOf<Int?>(null) }
    var confirmarId by rememberSaveable { mutableStateOf<Int?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val confirmar = pedidos.firstOrNull { it.id == confirmarId && it.statusEntrega == "AGUARDANDO_PREPARO" }

    if (confirmar != null) {
        AlertDialog(onDismissRequest = { if (processando == null) { confirmarId = null; erro = null } },
            title = { Text("Pedido pronto para retirada?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("O pedido #${confirmar.id} ficará disponível para um entregador coletar. Confirme apenas se todos os produtos estiverem prontos.")
                    erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = {
                Button(onClick = {
                    if (processando == null) {
                        processando = confirmar.id
                        erro = null
                        scope.launch {
                            try { onPronto(confirmar.id); confirmarId = null }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { erro = e.message ?: "Não foi possível liberar o pedido. Tente novamente." }
                            finally { processando = null }
                        }
                    }
                }, enabled = processando == null) { Text(if (processando != null) "Liberando..." else "Confirmar") }
            }, dismissButton = {
                TextButton(onClick = { confirmarId = null; erro = null }, enabled = processando == null) { Text("Cancelar") }
            })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Pedidos recebidos") }, navigationIcon = {
        IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, "Voltar") }
    }) }) { padding ->
        if (pedidos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Você ainda não recebeu pedidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (confirmar == null) erro?.let { mensagem -> item { Text(mensagem, color = MaterialTheme.colorScheme.error) } }
                items(pedidos, key = { it.id }) { pedido ->
                    PedidoRecebidoCard(pedido, processando == null,
                        onProntoClick = { erro = null; confirmarId = pedido.id })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PedidoRecebidoCard(pedido: PedidoEntity, podeAlterar: Boolean, onProntoClick: () -> Unit) {
    var detalhes by rememberSaveable(pedido.id) { mutableStateOf(false) }
    val corStatus = if (pedido.statusEntrega == "ENTREGUE") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    Card(modifier = Modifier.fillMaxWidth().clickable { detalhes = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Pedido #${pedido.id}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("R$ ${pedido.valorTotal}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("Status: ${descricaoStatus(pedido.statusEntrega)}", color = corStatus,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pedido.emailCliente, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(pedido.resumoItens, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (pedido.statusEntrega == "AGUARDANDO_PREPARO") {
                Button(onClick = onProntoClick, enabled = podeAlterar,
                    modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) { Text("Pronto para retirada") }
            }
        }
    }
    if (detalhes) {
        ModalBottomSheet(onDismissRequest = { detalhes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Pedido #${pedido.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(descricaoStatus(pedido.statusEntrega), color = corStatus, fontWeight = FontWeight.SemiBold)
                Text("Cliente", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pedido.emailCliente)
                Text("Produtos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pedido.resumoItens)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text("R$ ${pedido.valorTotal}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (pedido.statusEntrega == "AGUARDANDO_PREPARO") {
                    Button(onClick = { detalhes = false; onProntoClick() }, enabled = podeAlterar,
                        modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) { Text("Pronto para retirada") }
                }
            }
        }
    }
}
