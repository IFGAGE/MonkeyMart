package com.example.marketplace.ui.acompanhamento

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.mapa.MapaAcompanhamento
import com.example.marketplace.ui.mapa.consultarTrajetoDaEntrega
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AcompanharEntregaScreen(pedido: PedidoEntity, previa: Boolean, onVoltar: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var leitura by remember(pedido.firestoreId, previa) { mutableStateOf(LeituraRastreamento()) }
    val locais by RastreamentoEntrega.posicoes.collectAsState()
    val ponto = (if (previa) locais[pedido.firestoreId] else leitura.posicao)?.takeIf { it.corresponde(pedido) }
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    var trajeto by remember(pedido.firestoreId) { mutableStateOf(emptyList<Coordenadas>()) }
    var erroTrajeto by remember { mutableStateOf(false) }
    val pontoAtual by rememberUpdatedState(ponto)
    val avisos = remember { SnackbarHostState() }
    var avisado by rememberSaveable(pedido.firestoreId) { mutableStateOf(false) }

    LaunchedEffect(pedido.firestoreId, previa, lifecycle) {
        if (!previa) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            observarRastreamento(pedido.firestoreId).collect { leitura = it }
        }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { agora = System.currentTimeMillis(); delay(1_000) }
        }
    }
    // A posição e os metros continuam atualizando enquanto a consulta de ruas é feita separadamente.
    LaunchedEffect(pedido.firestoreId, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var origemAnterior: Coordenadas? = null
            while (isActive) {
                val atual = pontoAtual?.takeIf { it.atual(System.currentTimeMillis()) }
                if (atual != null && (origemAnterior == null || distanciaMetros(origemAnterior!!, atual.coordenadas) >= 40 || trajeto.isEmpty())) {
                    try {
                        trajeto = consultarTrajetoDaEntrega(atual.coordenadas, Coordenadas(atual.destinoLatitude, atual.destinoLongitude))
                        origemAnterior = atual.coordenadas
                        erroTrajeto = false
                    } catch (cancelamento: CancellationException) { throw cancelamento }
                    catch (_: Exception) { erroTrajeto = true }
                }
                delay(10_000)
            }
        }
    }
    LaunchedEffect(pedido.avisoProximidade, previa) {
        if (previa && pedido.avisoProximidade && pedido.statusEntrega == "EM_ROTA" && !avisado) {
            avisado = true
            avisos.showSnackbar("Prévia: seu entregador está perto do endereço de entrega.")
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Acompanhar entrega") }, navigationIcon = {
        IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
    }) }, snackbarHost = { SnackbarHost(avisos) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (previa) Text("Prévia do comprador • teste neste celular", style = MaterialTheme.typography.labelLarge)
            Text("Pedido #${pedido.id}", style = MaterialTheme.typography.titleLarge)
            Text(pedido.enderecoEntrega)
            when {
                pedido.statusEntrega == "ENTREGUE" -> Text("Pedido entregue. Acompanhamento encerrado.")
                pedido.statusEntrega != "EM_ROTA" -> Text("A entrega ainda não foi iniciada.")
                ponto == null -> Text(leitura.erro ?: "Aguardando a localização do entregador…")
                else -> {
                    val atualizado = ponto.atual(maxOf(agora, System.currentTimeMillis()))
                    Text("${ponto.distanciaMetros().roundToInt()} m até a entrega", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Text(when {
                        !atualizado -> "Última posição conhecida. Aguardando o entregador voltar a compartilhar."
                        previa && ponto.simulada -> "Posição simulada recebida do Android"
                        previa -> "Posição real recebida do Android"
                        else -> "Localização atualizada automaticamente"
                    },
                        color = if (atualizado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                    Text("Distância em linha reta até o endereço de entrega.", style = MaterialTheme.typography.bodySmall)
                    if (pedido.avisoProximidade) Text("Seu entregador está perto do endereço de entrega.",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    MapaAcompanhamento(ponto.coordenadas, Coordenadas(ponto.destinoLatitude, ponto.destinoLongitude),
                        trajeto, Modifier.fillMaxWidth().weight(1f))
                    if (erroTrajeto) Text("Trajeto por ruas indisponível. A posição e a distância continuam sendo exibidas.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Marcadores: entregador e destino. Círculo verde: área de 200 m.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
