package com.example.marketplace.ui.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.marketplace.data.notificacoes.Notificacao
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SinoNotificacoes(naoLidas: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            if (naoLidas > 0) Badge()
        }) {
            Icon(Icons.Default.Notifications, contentDescription =
                if (naoLidas > 0) "Notificações ($naoLidas não lidas)" else "Notificações")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificacoesScreen(
    notificacoes: List<Notificacao>,
    onVoltar: () -> Unit,
    onLer: (String) -> Unit,
    onLimpar: () -> Unit
) {
    var confirmarLimpeza by rememberSaveable { mutableStateOf(false) }
    if (confirmarLimpeza) AlertDialog(
        onDismissRequest = { confirmarLimpeza = false },
        title = { Text("Limpar histórico?") },
        text = { Text("Os avisos serão apagados neste aparelho.") },
        confirmButton = {
            TextButton(onClick = { confirmarLimpeza = false; onLimpar() }) { Text("Limpar") }
        },
        dismissButton = { TextButton(onClick = { confirmarLimpeza = false }) { Text("Cancelar") } }
    )
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Notificações") },
            navigationIcon = {
                IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            },
            actions = {
                if (notificacoes.isNotEmpty()) TextButton(onClick = { confirmarLimpeza = true }) { Text("Limpar") }
            }
        )
    }) { padding ->
        if (notificacoes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Nenhuma notificação no histórico.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notificacoes, key = { it.id }) { notificacao ->
                    Card(
                        Modifier.fillMaxWidth().semantics {
                            stateDescription = if (notificacao.lida) "Lida" else "Não lida"
                        }.clickable(enabled = !notificacao.lida, onClickLabel = "Marcar como lida") {
                            onLer(notificacao.id)
                        }
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!notificacao.lida) {
                                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                }
                                Text(notificacao.titulo, style = MaterialTheme.typography.titleMedium)
                            }
                            notificacao.pedidoId?.let { Text("Pedido #$it", style = MaterialTheme.typography.labelLarge) }
                            Text(notificacao.mensagem, style = MaterialTheme.typography.bodyMedium)
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(notificacao.recebidaEm)), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
