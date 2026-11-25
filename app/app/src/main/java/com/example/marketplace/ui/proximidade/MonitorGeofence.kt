package com.example.marketplace.ui.proximidade

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.marketplace.data.entity.PedidoEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import java.util.UUID

internal object MonitorGeofence {
    // Configuração e mensagens usadas também pelo controle de permissões da rota.
    const val RAIO = 200f
    private val trava = Mutex()
    private val revisao = MutableStateFlow(0)
    val revisoes = revisao.asStateFlow()
    val mensagem = MutableStateFlow("Inicie a rota para monitorar a aproximação.")

    fun estado(context: Context) = context.getSharedPreferences("geofences_entregas", Context.MODE_PRIVATE)
    fun revisar() { revisao.update { it + 1 } }

    fun temPermissoes(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)

    // O Android usará esta ação para entregar ENTER ao GeofenceReceiver.
    private fun eventoPendente(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, 20, Intent(context, GeofenceReceiver::class.java), flags)
    }

    // ProximidadeEntrega chama quando a rota, os pedidos ou as permissões mudam.
    @SuppressLint("MissingPermission")
    suspend fun atualizar(context: Context, email: String?, pedidos: List<PedidoEntity>, forcar: Boolean = false) {
        // Conclui a troca de registros mesmo se a tela sair durante a chamada ao serviço.
        withContext(NonCancellable) {
            trava.withLock {
                val estado = estado(context)
                val usuario = FirebaseAuth.getInstance().currentUser
                val destinos = pedidos.filter { it.emailEntregador == email && destinoValido(it) }.sortedBy { it.id }
                val autorizado = usuario != null && usuario.email == email && temPermissoes(context)
                val assinatura = "$email|${destinos.joinToString(";") { "${it.id}:${it.entregaLatitude},${it.entregaLongitude}" }}"
                val cliente = LocationServices.getGeofencingClient(context)
                try {
                    if (!autorizado || destinos.isEmpty() || destinos.size > 100) {
                        val existia = estado.contains("token")
                        estado.edit().clear().commit() // Invalida eventos antes de remover o registro.
                        if (existia) withTimeout(12_000) { cliente.removeGeofences(eventoPendente(context)).await() }
                        mensagem.value = when {
                            destinos.size > 100 -> "A rota ultrapassa o limite de 100 áreas monitoradas."
                            destinos.isNotEmpty() && !autorizado -> "Permita localização precisa e o acesso o tempo todo."
                            else -> "Nenhuma entrega aguardando aproximação."
                        }
                        return@withLock
                    }
                    if (!forcar && estado.getString("assinatura", null) == assinatura &&
                        estado.getBoolean("registrado", false) && estado.getString("usuario", null) == usuario!!.uid) return@withLock
                    val token = UUID.randomUUID().toString()
                    val salvar = estado.edit().clear().putString("token", token).putString("email", email)
                        .putString("usuario", usuario!!.uid).putString("assinatura", assinatura)
                    destinos.forEach { salvar.putString("ponto_${it.id}", "${it.entregaLatitude},${it.entregaLongitude}") }
                    salvar.commit()
                    withTimeout(12_000) { cliente.removeGeofences(eventoPendente(context)).await() }
                    val areas = destinos.map { pedido ->
                        Geofence.Builder().setRequestId("$token:${pedido.id}")
                            .setCircularRegion(pedido.entregaLatitude!!, pedido.entregaLongitude!!, RAIO)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setNotificationResponsiveness(5_000).build()
                    }
                    val solicitacao = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofences(areas).build()
                    withTimeout(12_000) { cliente.addGeofences(solicitacao, eventoPendente(context)).await() }
                    estado.edit().putBoolean("registrado", true).apply()
                    mensagem.value = "Aproximação ativa • raio de ${RAIO.toInt()} m."
                } catch (erro: Exception) {
                    estado.edit().clear().commit()
                    mensagem.value = "Não foi possível ativar a aproximação. Confira o GPS e tente novamente."
                    android.util.Log.e("Geofence", "Falha ao atualizar áreas de entrega", erro)
                }
            }
        }
    }
}

internal fun destinoValido(pedido: PedidoEntity): Boolean =
    pedido.statusEntrega == "EM_ROTA" && !pedido.avisoProximidade &&
        pedido.entregaLatitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
        pedido.entregaLongitude?.let { it.isFinite() && it in -180.0..180.0 } == true

internal fun idDoEvento(requestId: String, tokenAtual: String): Int? {
    if (tokenAtual.isBlank() || !requestId.startsWith("$tokenAtual:")) return null
    return requestId.substringAfter(':').toIntOrNull()?.takeIf { it > 0 }
}
