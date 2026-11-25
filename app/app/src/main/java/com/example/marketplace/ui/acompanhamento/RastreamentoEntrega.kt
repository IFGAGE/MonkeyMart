package com.example.marketplace.ui.acompanhamento

import android.location.Location
import android.os.SystemClock
import com.example.marketplace.data.AppDatabase
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update

internal object RastreamentoEntrega {
    val posicoes = MutableStateFlow<Map<String, PosicaoEntrega>>(emptyMap())
    val mensagem = MutableStateFlow("Aguardando posição do entregador…")
    // Mantém só um envio em andamento: offline não cria uma fila de posições a cada segundo.
    private var envio: Task<Void>? = null

    fun publicar(pontos: Map<String, PosicaoEntrega>) {
        posicoes.update { it + pontos }
        if (envio?.isComplete == false || pontos.isEmpty()) return
        val firestore = FirebaseFirestore.getInstance()
        val lote = firestore.batch()
        pontos.forEach { (id, ponto) ->
            // Atualiza apenas o rastreamento; não altera coleta, entrega, revisão ou aviso.
            lote.update(firestore.collection("pedidos").document(id), "rastreamento", ponto.dados())
        }
        envio = lote.commit().addOnSuccessListener { mensagem.value = "Localização compartilhada com o comprador." }
            .addOnFailureListener { mensagem.value = "Sem envio da localização. Verifique a conexão e o acesso ao pedido." }
    }

    fun limpar() {
        posicoes.value = emptyMap()
        envio = null
    }
}

internal data class LeituraRastreamento(val posicao: PosicaoEntrega? = null, val erro: String? = null)

internal fun observarRastreamento(firestoreId: String) = callbackFlow {
    val listener = FirebaseFirestore.getInstance().collection("pedidos").document(firestoreId)
        .addSnapshotListener { snapshot, erro ->
            trySend(if (erro != null) LeituraRastreamento(erro = "Não foi possível receber a localização. Verifique sua conexão e tente novamente.")
                else LeituraRastreamento(lerPosicao(snapshot?.get("rastreamento") as? Map<*, *>)))
        }
    awaitClose { listener.remove() }
}

// Chamado a cada posição recebida pelo serviço, independentemente dos cliques nas flags.
internal suspend fun processarPosicaoEntrega(
    db: AppDatabase,
    email: String,
    latitude: Double,
    longitude: Double,
    precisao: Float,
    capturadaEm: Long,
    agora: Long = System.currentTimeMillis(),
    simulada: Boolean = false
): Pair<Map<String, PosicaoEntrega>, List<Int>> {
    if (!coordenadaValida(latitude, longitude) || !precisao.isFinite() || precisao !in 0f..100f ||
        agora - capturadaEm !in 0L..30_000L) return emptyMap<String, PosicaoEntrega>() to emptyList()
    val pedidos = db.pedidoDao().carregarEntregasDaRota(email)
    val pontos = pedidos.mapNotNull { pedido ->
        val lat = pedido.entregaLatitude ?: return@mapNotNull null
        val lon = pedido.entregaLongitude ?: return@mapNotNull null
        if (!coordenadaValida(lat, lon)) return@mapNotNull null
        pedido to PosicaoEntrega(latitude, longitude, precisao, capturadaEm, email, lat, lon, simulada)
    }
    val proximos = pontos.filter { (pedido, ponto) -> !pedido.avisoProximidade && podeRegistrarAproximacao(ponto, agora) }
        .associate { (pedido, _) -> pedido.id to "${pedido.entregaLatitude},${pedido.entregaLongitude}" }
    val avisos = if (proximos.isEmpty()) emptyList() else db.pedidoDao().registrarEntradaGeofence(email, proximos)
    return pontos.associate { (pedido, ponto) -> pedido.firestoreId to ponto } to avisos
}

internal fun Location.instanteDaMedicao(agora: Long = System.currentTimeMillis()): Long? {
    if (!hasAccuracy()) return null
    val idade = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    return if (idade in 0L..30_000L) agora - idade else null
}
