package com.example.marketplace.ui.proximidade

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.marketplace.MainActivity
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.notificacoes.HistoricoNotificacoes
import com.example.marketplace.data.sync.sincronizarPedidos
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class GeofenceReceiver : BroadcastReceiver() {
    // Chamado pelo Android, inclusive ao alternar para o Mock Locations.
    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) {
            MonitorGeofence.estado(context).edit().putBoolean("registrado", false).apply()
            MonitorGeofence.mensagem.value = "Monitoramento interrompido: ${GeofenceStatusCodes.getStatusCodeString(evento.errorCode)}. Tente ativar novamente."
            return
        }
        if (evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val estado = MonitorGeofence.estado(context)
        val usuario = FirebaseAuth.getInstance().currentUser ?: return
        val email = usuario.email ?: return
        if (estado.getString("usuario", null) != usuario.uid || estado.getString("email", null) != email) return
        val token = estado.getString("token", "") ?: return
        val pontos = evento.triggeringGeofences.orEmpty().mapNotNull { area ->
            val id = idDoEvento(area.requestId, token) ?: return@mapNotNull null
            estado.getString("ponto_$id", null)?.let { id to it }
        }.toMap()
        if (pontos.isEmpty()) return
        val pendente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000) {
                    // Confere de novo, pois a conta/rota pode mudar entre a chegada e o processamento.
                    if (FirebaseAuth.getInstance().currentUser?.uid != usuario.uid ||
                        estado.getString("token", null) != token) return@withTimeoutOrNull
                    val db = AppDatabase.getDatabase(context)
                    val alterados = db.pedidoDao().registrarEntradaGeofence(email, pontos)
                    if (alterados.isNotEmpty()) {
                        MonitorGeofence.mensagem.value = "Entrada detectada. Aproximação registrada para o comprador."
                        alterados.forEach { notificarRegistro(context, usuario.uid, it) }
                        // O aviso fica salvo no Room mesmo offline; o fluxo normal também tenta reenviá-lo.
                        withTimeoutOrNull(4_000) { sincronizarPedidos(db.pedidoDao()) }
                    }
                }
            } catch (erro: Exception) {
                android.util.Log.e("Geofence", "Aviso local ou sincronização não concluídos", erro)
            } finally {
                pendente.finish()
            }
        }
    }

}

// Confirmação no celular do entregador. O comprador recebe o aviso por sincronização do pedido.
internal fun notificarRegistro(context: Context, usuarioId: String, pedidoId: Int) {
    if (FirebaseAuth.getInstance().currentUser?.uid != usuarioId) return
    val titulo = "Aproximação registrada"
    val mensagem = "Você entrou na área de uma entrega. O aviso foi salvo para o comprador."
    HistoricoNotificacoes.registrar(context, usuarioId, "entregador:$pedidoId", titulo, mensagem, pedidoId)

    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
        NotificationChannel("aproximacao_entregas", "Aproximação de entregas", NotificationManager.IMPORTANCE_DEFAULT))
    val abrir = PendingIntent.getActivity(context, 20, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    try {
        manager.notify("proximidade:$pedidoId", 20, NotificationCompat.Builder(context, "aproximacao_entregas")
            .setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("$titulo · Pedido #$pedidoId")
            .setContentText(mensagem)
            .setContentIntent(abrir).setAutoCancel(true).build())
    } catch (_: SecurityException) { /* A permissão pode ter sido revogada após a conferência. */ }
}
