package com.example.marketplace.data.sync

import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.mapa.pedidoIds
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val reservaMutex = Mutex()

// Serializa iniciar/cancelar mesmo quando a pessoa sai e reabre a tela durante a operação.
// A transação remota impede dois motoristas de iniciarem os mesmos pedidos.
suspend fun iniciarRotaComReserva(db: AppDatabase, email: String, placa: String, ids: List<Int>) = reservaMutex.withLock {
    require(email.isNotBlank() && ids.isNotEmpty())
    check(db.veiculoDao().buscarVeiculosDoEntregador(email).first().any { it.placa == placa }) {
        "Cadastre e selecione um veículo válido."
    }
    val pedidos = db.pedidoDao().buscarPorIds(ids).first()
    check(pedidos.size == ids.size)
    // O estado sobrevive a fechamento do app ou perda da resposta da reserva.
    check(db.rotaDao().marcarReservando(email, placa) == 1) { "Rota planejada não encontrada." }
    try {
        withTimeout(25_000) { sincronizarPedidos(db.pedidoDao()) }
    } catch (e: TimeoutCancellationException) {
        throw IOException("Não foi possível verificar os pedidos a tempo. Verifique a conexão e tente novamente.", e)
    }
    // Cancelar await não cancela uma transação Firebase. Aguarda seu desfecho antes
    // de liberar os controles; após morte do processo, RESERVANDO permite retomar ou desfazer.
    withContext(NonCancellable) {
        val firestore = Firebase.firestore
        val refs = pedidos.map { firestore.collection("pedidos").document(it.firestoreId) }
        val reservados = firestore.runTransaction { transaction ->
            val remotos = refs.map { ref ->
                val snapshot = transaction.get(ref)
                checkNotNull(snapshot.data?.let { pedidoRemoto(snapshot.id, it) }) { "Pedido ainda não sincronizado." }
            }
            check(remotos.all { it.statusEntrega == "PRONTO_RETIRADA" && it.emailEntregador.isBlank() ||
                it.statusEntrega == "EM_ROTA" && it.emailEntregador == email }) {
                "Um dos pedidos já foi assumido por outro motorista. Atualize a rota."
            }
            remotos.mapIndexed { index, pedido ->
                val reservado = if (pedido.statusEntrega == "EM_ROTA") pedido else pedido.copy(
                    statusEntrega = "EM_ROTA", emailEntregador = email, revisao = pedido.revisao + 1)
                transaction.set(refs[index], dadosPedido(reservado))
                reservado
            }
        }.await()
        reservados.forEach { db.pedidoDao().receberPedido(it) }
        db.rotaDao().iniciar(email, placa, ids)
    }
}

suspend fun excluirRotaPlanejadaOuReserva(db: AppDatabase, email: String, ids: List<Int>) = reservaMutex.withLock {
    excluirPlanejadaOuReserva(db, email, ids)
}

suspend fun cancelarRotaDoEntregador(db: AppDatabase, esperada: RotaEntity) = reservaMutex.withLock {
    val email = esperada.emailEntregador
    check(db.rotaDao().observar(email).first() == esperada) { "A rota mudou. Confira novamente antes de cancelar." }
    if (esperada.estado == "INICIADA") db.rotaDao().cancelar(email, esperada.pedidoIds())
    else excluirPlanejadaOuReserva(db, email, esperada.pedidoIds())
}

private suspend fun excluirPlanejadaOuReserva(db: AppDatabase, email: String, ids: List<Int>) {
    val rota = checkNotNull(db.rotaDao().observar(email).first())
    if (rota.estado == "PLANEJADA") {
        db.rotaDao().excluirPlanejada(email)
        return
    }
    check(rota.estado == "RESERVANDO") { "Essa rota já foi iniciada." }
    val pedidos = db.pedidoDao().buscarPorIds(ids).first()
    check(pedidos.size == ids.size)
    withContext(NonCancellable) {
        val firestore = Firebase.firestore
        val refs = pedidos.map { firestore.collection("pedidos").document(it.firestoreId) }
        val liberados = firestore.runTransaction { transaction ->
            val remotos = refs.map { ref ->
                val snapshot = transaction.get(ref)
                snapshot.data?.let { pedidoRemoto(snapshot.id, it) }
            }
            remotos.mapIndexedNotNull { index, pedido ->
                check(pedido == null || pedido.emailEntregador != email || !pedido.coletado) { "Há pedidos já coletados nesta rota." }
                if (pedido == null) null
                else if (pedido.statusEntrega == "EM_ROTA" && pedido.emailEntregador == email ||
                    pedido.statusEntrega == "PRONTO_RETIRADA" && pedido.emailEntregador.isBlank()) {
                    // Atualizar a versão também invalida tentativas antigas de reserva.
                    val liberado = pedido.copy(statusEntrega = "PRONTO_RETIRADA", emailEntregador = "",
                        revisao = pedido.revisao + 1)
                    transaction.set(refs[index], dadosPedido(liberado))
                    liberado
                } else pedido
            }
        }.await()
        liberados.forEach { db.pedidoDao().receberPedido(it) }
        check(db.rotaDao().excluirReservaResolvida(email) == 1)
    }
}
