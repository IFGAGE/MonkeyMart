package com.example.marketplace.ui.proximidade

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.marketplace.MainActivity
import com.example.marketplace.data.dao.PedidoDao
import com.example.marketplace.data.notificacoes.HistoricoNotificacoes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine

private const val CANAL_COMPRADOR = "avisos_comprador"

@Composable
internal fun NotificarComprador(dao: PedidoDao, usuarioId: String, email: String) {
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val entregues = remember(context, usuarioId) {
        context.getSharedPreferences("notificacoes_android_comprador_$usuarioId", Context.MODE_PRIVATE)
    }
    var revisaoPermissao by remember { mutableIntStateOf(0) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revisaoPermissao++
    }
    DisposableEffect(ciclo) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) revisaoPermissao++
        }
        ciclo.addObserver(observer)
        onDispose { ciclo.removeObserver(observer) }
    }
    LaunchedEffect(context) {
        val configuracao = context.getSharedPreferences("notificacoes_android", Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !configuracao.getBoolean("compradorSolicitouPermissao", false) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            configuracao.edit().putBoolean("compradorSolicitouPermissao", true).apply()
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // O Room recebe os pedidos do listener do Firestore. A bandeja independe do aviso dentro da tela.
    LaunchedEffect(dao, context, usuarioId, email) {
        combine(dao.observarAvisosDeProximidade(email), snapshotFlow { revisaoPermissao }) { pedidos, _ -> pedidos }
            .collect { pedidos ->
                if (FirebaseAuth.getInstance().currentUser?.uid != usuarioId) return@collect
                for (pedido in pedidos) {
                    if (pedido.statusEntrega != "EM_ROTA" || entregues.getBoolean(pedido.firestoreId, false) ||
                        HistoricoNotificacoes.foiLimpa(context, usuarioId, "comprador:${pedido.firestoreId}")) continue

                    if (mostrarAvisoNoAndroid(context, usuarioId, pedido.firestoreId, pedido.pedidoId)) {
                        entregues.edit().putBoolean(pedido.firestoreId, true).apply()
                    }
                }
            }
    }
}

private fun mostrarAvisoNoAndroid(context: Context, usuarioId: String, firestoreId: String, pedidoId: Int): Boolean {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false

    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT >= 26) {
        val sistema = context.getSystemService(NotificationManager::class.java)
        sistema.createNotificationChannel(NotificationChannel(
            CANAL_COMPRADOR, "Avisos das suas compras", NotificationManager.IMPORTANCE_DEFAULT
        ))
        if (sistema.getNotificationChannel(CANAL_COMPRADOR)?.importance == NotificationManager.IMPORTANCE_NONE) return false
    }
    val abrir = PendingIntent.getActivity(
        context, 22, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val aviso = NotificationCompat.Builder(context, CANAL_COMPRADOR)
        .setSmallIcon(android.R.drawable.ic_dialog_map)
        .setContentTitle("Entregador por perto · Pedido #$pedidoId")
        .setContentText("Seu entregador está perto do endereço de entrega.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setContentIntent(abrir)
        .setAutoCancel(true)
        .build()

    return try {
        manager.notify("comprador:$usuarioId:$firestoreId", 22, aviso)
        true
    } catch (_: SecurityException) {
        false
    }
}
