package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.mapa.*
import com.example.marketplace.ui.screens.profile.VeiculoTemp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotaEntregaScreen(rota: RotaEntity, rotaGerada: RotaGerada, pedidos: List<PedidoTemp>,
    veiculos: List<VeiculoTemp>, processando: Boolean, erro: String?, onVoltar: () -> Unit,
    onCadastrarVeiculo: () -> Unit, onExcluir: () -> Unit, onIniciar: (String) -> Unit,
    onAtualizarParada: (List<Int>, TipoParada, Boolean) -> Unit,
    onSelecionarVeiculo: suspend (String) -> Unit,
    onCancelar: () -> Unit, onFinalizar: () -> Unit, onRecalcular: () -> Unit) {
    var placaSelecionada by rememberSaveable(rota.emailEntregador) { mutableStateOf(rota.placaVeiculo) }
    var escolherVeiculo by rememberSaveable(rota.emailEntregador) {
        mutableStateOf(rota.estado == "PLANEJADA" && rota.placaVeiculo.isNullOrBlank())
    }
    var salvandoVeiculo by remember { mutableStateOf(false) }
    var erroVeiculo by remember { mutableStateOf<String?>(null) }
    var confirmarCancelamento by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // A câmera pertence à tela. A View nativa segue o reuso da LazyColumn,
    // preservando zoom e posição mesmo se precisar ser recriada.
    val estadoMapa = lembrarEstadoMapaRota()
    val iniciada = rota.estado == "INICIADA"
    val bloqueado = processando || salvandoVeiculo
    val idsDaRota = rotaGerada.paradas.flatMap { it.pedidoIds }.toSet()
    val pedidosPorId = pedidos.associateBy { it.id }
    val todosConcluidos = idsDaRota.isNotEmpty() && idsDaRota.all {
        pedidosPorId[it]?.let { p -> p.coletado && p.statusEntrega == "ENTREGUE" } == true
    }
    val podeCancelar = !iniciada || (pedidosPorId.keys.containsAll(idsDaRota) && pedidos.none { it.coletado })
    val veiculo = veiculos.find { it.placa == placaSelecionada }

    if (confirmarCancelamento) ConfirmarCancelamentoRota(
        processando = bloqueado, onDismiss = { confirmarCancelamento = false },
        onConfirmar = { confirmarCancelamento = false; if (iniciada) onCancelar() else onExcluir() },
        entregasIniciadas = iniciada)

    if (rota.estado == "PLANEJADA" && escolherVeiculo) {
        AlertDialog(onDismissRequest = { if (!salvandoVeiculo) onVoltar() }, title = { Text("Veículo das entregas") },
            text = {
                Column {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        if (veiculos.isEmpty()) item { Text("Cadastre um veículo para iniciar as entregas.") }
                        items(veiculos, key = { it.placa }) { opcao ->
                            Row(Modifier.fillMaxWidth().clickable(enabled = !salvandoVeiculo) { placaSelecionada = opcao.placa },
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(placaSelecionada == opcao.placa, { placaSelecionada = opcao.placa }, enabled = !salvandoVeiculo)
                                Text("${opcao.modelo} • ${opcao.placa}")
                            }
                        }
                    }
                    erroVeiculo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = {
                if (veiculos.isEmpty()) TextButton(onClick = onCadastrarVeiculo) { Text("Cadastrar veículo") }
                else Button(onClick = {
                    val placa = placaSelecionada ?: return@Button
                    scope.launch {
                        salvandoVeiculo = true
                        erroVeiculo = null
                        try { onSelecionarVeiculo(placa); escolherVeiculo = false }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { erroVeiculo = e.message ?: "Não foi possível salvar o veículo." }
                        finally { salvandoVeiculo = false }
                    }
                }, enabled = veiculo != null && !bloqueado) { Text(if (salvandoVeiculo) "Salvando..." else "Usar este veículo") }
            }, dismissButton = { TextButton(onClick = onVoltar, enabled = !salvandoVeiculo) { Text("Voltar") } })
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (iniciada) "Suas entregas" else "Rota planejada") },
            navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, "Voltar") } })
    }, bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (iniciada) {
                    Button(onClick = onFinalizar, enabled = todosConcluidos && !bloqueado, modifier = Modifier.fillMaxWidth()) {
                        Text(if (processando) "Aguarde..." else "Encerrar entregas")
                    }
                    if (!todosConcluidos) Text("Confirme todas as coletas e entregas para encerrar.",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                } else {
                    Button(onClick = { rota.placaVeiculo?.let(onIniciar) },
                        enabled = veiculo != null && !escolherVeiculo && !bloqueado && pedidosPorId.keys.containsAll(idsDaRota),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (processando) "Aguarde..." else if (rota.estado == "RESERVANDO") "Tentar iniciar entregas novamente" else "Iniciar entregas")
                    }
                    if (rota.estado == "PLANEJADA") OutlinedButton(onClick = onRecalcular,
                        enabled = !bloqueado && pedidosPorId.keys.containsAll(idsDaRota), modifier = Modifier.fillMaxWidth()) { Text("Recalcular rota") }
                }
                if (podeCancelar) TextButton(onClick = { confirmarCancelamento = true }, enabled = !bloqueado,
                    modifier = Modifier.fillMaxWidth()) { Text(if (iniciada) "Cancelar entregas" else "Cancelar rota") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).clipToBounds(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "mapa-rota") { MapaRota(rotaGerada, Modifier.fillMaxWidth().height(240.dp), estadoMapa) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${rotaGerada.paradas.size} paradas • ${idsDaRota.size} pedidos", style = MaterialTheme.typography.titleMedium)
                        Text(rota.placaVeiculo?.let { "Veículo: $it" } ?: "Selecione o veículo das entregas")
                        if (rota.estado == "PLANEJADA") TextButton(onClick = { escolherVeiculo = true }, enabled = !bloqueado) { Text("Alterar veículo") }
                        if (rota.estado == "RESERVANDO") Text("O início está pendente. Tente novamente ou cancele a rota.")
                    }
                }
            }
            erro?.let { mensagem -> item { Text(mensagem, color = MaterialTheme.colorScheme.error) } }
            item { Text("Paradas", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(rotaGerada.paradas, key = { indice, parada -> "$indice-${parada.tipo}-${parada.pedidoIds}" }) { indice, parada ->
                var expandida by rememberSaveable { mutableStateOf(false) }
                val progresso = progressoParada(parada, pedidosPorId)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().clickable { expandida = !expandida }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (iniciada) TriStateCheckbox(
                            state = if (progresso.concluida) ToggleableState.On else if (progresso.concluidos > 0) ToggleableState.Indeterminate else ToggleableState.Off,
                            onClick = { onAtualizarParada(parada.pedidoIds, parada.tipo, !progresso.concluida) },
                            enabled = !bloqueado && progresso.podeAlterar)
                        Icon(if (parada.tipo == TipoParada.COLETA) Icons.Default.Storefront else Icons.Default.LocationOn, null)
                        Column(Modifier.weight(1f)) {
                            Text("${indice + 1}. ${if (parada.tipo == TipoParada.COLETA) "Coleta" else "Entrega"}", style = MaterialTheme.typography.titleMedium)
                            Text(parada.endereco, style = MaterialTheme.typography.bodySmall)
                            if (iniciada) Text("${progresso.concluidos}/${parada.pedidoIds.size} pedidos ${if (parada.tipo == TipoParada.COLETA) "coletados" else "entregues"}",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Icon(if (expandida) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expandida) "Recolher pedidos" else "Ver pedidos")
                    }
                    if (expandida) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parada.pedidoIds.forEach { id ->
                            val pedido = pedidosPorId[id]
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (iniciada && pedido != null) {
                                    Checkbox(checked = if (parada.tipo == TipoParada.COLETA) pedido.coletado else pedido.statusEntrega == "ENTREGUE",
                                        onCheckedChange = { onAtualizarParada(listOf(id), parada.tipo, it) },
                                        enabled = !bloqueado && if (parada.tipo == TipoParada.COLETA) pedido.statusEntrega == "EM_ROTA" else pedido.coletado)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Pedido #$id", fontWeight = FontWeight.SemiBold)
                                    Text(pedido?.resumoItens ?: "Carregando pedido...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (iniciada && parada.tipo == TipoParada.ENTREGA && !progresso.podeAlterar)
                            Text("Confirme as coletas antes de marcar a entrega.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmarCancelamentoRota(processando: Boolean, onDismiss: () -> Unit, onConfirmar: () -> Unit,
    entregasIniciadas: Boolean) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (entregasIniciadas) "Cancelar as entregas?" else "Cancelar a rota planejada?") },
        text = { Text(if (entregasIniciadas)
            "As entregas em andamento serão canceladas. Os pedidos voltarão para Aguardando entregador e ficarão disponíveis para outro entregador."
            else "O planejamento será removido e os pedidos ficarão disponíveis para outro entregador.") },
        confirmButton = { TextButton(onClick = onConfirmar, enabled = !processando) {
            Text(if (entregasIniciadas) "Cancelar entregas" else "Cancelar rota")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (entregasIniciadas) "Manter entregas" else "Manter rota") } })
}
