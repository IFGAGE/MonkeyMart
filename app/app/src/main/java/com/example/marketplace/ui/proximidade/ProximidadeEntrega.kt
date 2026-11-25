package com.example.marketplace.ui.proximidade

import android.content.Context
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.marketplace.ui.acompanhamento.ServicoRastreamento
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.notificacoes.HistoricoNotificacoes
import com.example.marketplace.data.notificacoes.MENSAGEM_PEDIDO_ENTREGUE
import com.example.marketplace.data.notificacoes.TITULO_PEDIDO_ENTREGUE
import com.example.marketplace.ui.mapa.pedidoIds
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ProximidadeEntrega(
    db: AppDatabase,
    usuarioId: String?,
    email: String?,
    abrirCompras: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val avisos = remember(usuarioId) { SnackbarHostState() }
    LaunchedEffect(lifecycle, usuarioId, email) {
        if (usuarioId == null || email.isNullOrBlank()) {
            ServicoRastreamento.atualizar(context, false)
            MonitorGeofence.atualizar(context, null, emptyList())
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            monitorarDestinos(context, db, email)
        }
    }
    LaunchedEffect(lifecycle, usuarioId, email) {
        if (usuarioId == null || email.isNullOrBlank()) return@LaunchedEffect
        val exibidos = context.getSharedPreferences("avisos_proximidade_$usuarioId", Context.MODE_PRIVATE)
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            db.pedidoDao().observarAvisosDeProximidade(email).distinctUntilChanged().collectLatest { pedidos ->
                val pendentes = pedidos.filterNot {
                    exibidos.getBoolean(it.firestoreId, false) ||
                        HistoricoNotificacoes.foiLimpa(context, usuarioId, "comprador:${it.firestoreId}")
                }.map { pedido ->
                    pedido to if (pedido.statusEntrega == "ENTREGUE")
                        MENSAGEM_PEDIDO_ENTREGUE
                    else "Seu entregador está perto do endereço de entrega."
                }
                for ((pedido, mensagem) in pendentes) {
                    HistoricoNotificacoes.registrar(context, usuarioId, "comprador:${pedido.firestoreId}",
                        if (pedido.statusEntrega == "ENTREGUE") TITULO_PEDIDO_ENTREGUE else "Entregador por perto",
                        mensagem, pedido.pedidoId)
                }
                for ((pedido, mensagem) in pendentes) {
                    if (HistoricoNotificacoes.foiLimpa(context, usuarioId, "comprador:${pedido.firestoreId}")) continue
                    exibidos.edit().putBoolean(pedido.firestoreId, true).apply()
                    val resultado = avisos.showSnackbar(
                        message = "Pedido #${pedido.pedidoId} · $mensagem",
                        actionLabel = "Ver compras",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                    if (resultado == SnackbarResult.ActionPerformed) abrirCompras()
                }
            }
        }
    }
    SnackbarHost(avisos, modifier.navigationBarsPadding())
}

// Ao voltar ao app, refaz o registro; ao sair da tela, o serviço mantém as áreas cadastradas.
private suspend fun monitorarDestinos(context: Context, db: AppDatabase, email: String) {
    var primeiroRegistro = true
    combine(db.rotaDao().observar(email), db.pedidoDao().observarEntregasEmAndamento(email),
        MonitorGeofence.revisoes) { rota, pedidos, _ ->
        val ids = if (rota?.estado == "INICIADA") {
            runCatching { rota.pedidoIds().toSet() }.getOrDefault(emptySet())
        } else emptySet()
        pedidos.filter { it.id in ids && it.emailEntregador == email }
    }.collect { destinos ->
        ServicoRastreamento.atualizar(context, destinos.isNotEmpty())
        MonitorGeofence.atualizar(context, email, destinos, forcar = primeiroRegistro)
        primeiroRegistro = false
    }
}
