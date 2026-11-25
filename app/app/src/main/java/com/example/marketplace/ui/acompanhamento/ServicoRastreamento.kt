package com.example.marketplace.ui.acompanhamento

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import com.example.marketplace.MainActivity
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.sync.sincronizarPedidos
import com.example.marketplace.ui.mapa.pedidoIds
import com.example.marketplace.ui.proximidade.MonitorGeofence
import com.example.marketplace.ui.proximidade.notificarRegistro
import com.example.marketplace.ui.proximidade.observarLocalizacao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect

// Mantém as posições chegando quando o usuário abre o Mock Locations ou apaga a tela.
class ServicoRastreamento : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var iniciado = false
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val usuario = FirebaseAuth.getInstance().currentUser
        val email = usuario?.email
        if (email.isNullOrBlank() || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        if (iniciado) return START_NOT_STICKY
        try { mostrarServicoAtivo() }
        catch (erro: RuntimeException) {
            RastreamentoEntrega.mensagem.value = "Não foi possível iniciar a localização. Volte à rota e confira as permissões."
            stopSelf()
            return START_NOT_STICKY
        }
        iniciado = true
        val uid = usuario.uid
        val db = AppDatabase.getDatabase(this)
        authListener = FirebaseAuth.AuthStateListener { if (it.currentUser?.uid != uid) stopSelf() }
            .also { FirebaseAuth.getInstance().addAuthStateListener(it) }
        scope.launch {
            combine(db.rotaDao().observar(email), db.pedidoDao().observarEntregasEmAndamento(email)) { rota, pedidos ->
                val ids = if (rota?.estado == "INICIADA") runCatching { rota.pedidoIds().toSet() }.getOrDefault(emptySet()) else emptySet()
                pedidos.any { it.id in ids }
            }.collect { temEntregas -> if (!temEntregas) stopSelf() }
        }
        scope.launch {
            try {
                observarLocalizacao(this@ServicoRastreamento).collect { localizacao ->
                    if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@collect
                    val instante = localizacao.instanteDaMedicao() ?: return@collect
                    val (pontos, avisos) = processarPosicaoEntrega(db, email, localizacao.latitude,
                        localizacao.longitude, localizacao.accuracy, instante, simulada = LocationCompat.isMock(localizacao))
                    if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@collect
                    // Nenhum clique de entrega participa deste caminho.
                    RastreamentoEntrega.publicar(pontos)
                    if (avisos.isNotEmpty()) {
                        MonitorGeofence.mensagem.value = "Aproximação detectada automaticamente. Aviso registrado."
                        avisos.forEach { notificarRegistro(this@ServicoRastreamento, uid, it) }
                        launch { sincronizar(db) }
                    }
                }
            } catch (cancelamento: CancellationException) { throw cancelamento }
            catch (erro: Exception) {
                RastreamentoEntrega.mensagem.value = "Sem posição atual. Confira o GPS e reabra a rota."
                stopSelf()
            }
        }
        scope.launch { while (isActive) { sincronizar(db); delay(5_000) } }
        return START_NOT_STICKY
    }

    private suspend fun sincronizar(db: AppDatabase) {
        try { withTimeoutOrNull(8_000) { sincronizarPedidos(db.pedidoDao()) } }
        catch (cancelamento: CancellationException) { throw cancelamento }
        catch (_: Exception) { /* O aviso permanece no Room para a próxima tentativa. */ }
    }

    private fun mostrarServicoAtivo() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("rastreamento_rota", "Entregas em andamento", NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(this, 21, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val aviso = NotificationCompat.Builder(this, "rastreamento_rota")
            .setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("Entrega em andamento")
            .setContentText("Compartilhando a posição para acompanhar suas entregas.")
            .setContentIntent(abrir).setOngoing(true).build()
        ServiceCompat.startForeground(this, 21, aviso,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
    }

    override fun onDestroy() {
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        scope.cancel()
        RastreamentoEntrega.limpar()
        super.onDestroy()
    }

    companion object {
        // Chamado com a tela ativa, após conceder localização; não inicia serviços do fundo.
        internal fun atualizar(context: Context, temEntregas: Boolean) {
            val intent = Intent(context, ServicoRastreamento::class.java)
            if (!temEntregas) { context.stopService(intent); return }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            try { ContextCompat.startForegroundService(context, intent) }
            catch (_: RuntimeException) { RastreamentoEntrega.mensagem.value = "Reabra a rota para iniciar o compartilhamento da localização." }
        }
    }
}
