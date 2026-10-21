package com.example.marketplace.data.sync

import android.util.Log
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.dao.AvaliacaoDao
import com.example.marketplace.data.dao.PedidoDao
import com.example.marketplace.data.dao.ProdutoDao
import com.example.marketplace.data.dao.UsuarioDao
import com.example.marketplace.data.dao.VeiculoDao
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.firebase.firestore.MetadataChanges


private val produtoSyncMutex = Mutex()

suspend fun sincronizarProdutos(produtoDao: ProdutoDao) = produtoSyncMutex.withLock {
    val firestore = Firebase.firestore
    val produtosPendentes = produtoDao.buscarNaoSincronizados()

    produtosPendentes.forEach { produto ->
        val ref = firestore.collection("produtos").document(produto.firestoreId)
        val atual = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val remoto = snapshot.data?.let { produtoRemoto(snapshot.id, it) }
            if (remoto == null || remoto.revisao < produto.revisao) {
                transaction.set(ref, produto)
                produto.copy(isSynced = true)
            } else remoto
        }.await()
        produtoDao.confirmarEnvio(produto)
        produtoDao.receberProduto(atual)
    }
}

suspend fun sincronizarUsuarios(usuarioDao: UsuarioDao) {
    val firestore = Firebase.firestore
    val pendentes = usuarioDao.buscarNaoSincronizados()

    pendentes.forEach { usuario ->
        firestore.collection("usuarios")
            .document(usuario.email)
            .set(usuario)
            .await()

        usuarioDao.marcarComoSincronizado(usuario.email)
    }
}

suspend fun sincronizarVeiculos(veiculoDao: VeiculoDao) {
    val firestore = Firebase.firestore
    val pendentes = veiculoDao.buscarNaoSincronizados()

    pendentes.forEach { veiculo ->
        firestore.collection("veiculos")
            .document(veiculo.placa)
            .set(veiculo)
            .await()

        veiculoDao.marcarComoSincronizado(veiculo.placa)
    }
}


private val pedidoSyncMutex = Mutex()

suspend fun sincronizarPedidos(pedidoDao: PedidoDao) = pedidoSyncMutex.withLock {
    val firestore = Firebase.firestore
    pedidoDao.buscarNaoSincronizados().forEach { pedido ->
        val ref = firestore.collection("pedidos").document(pedido.firestoreId)
        val atual = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val remoto = snapshot.data?.let { pedidoRemoto(snapshot.id, it) }
            if (remoto == null || remoto.revisao < pedido.revisao) {
                transaction.set(ref, dadosPedido(pedido))
                pedido.copy(isSynced = true)
            } else remoto
        }.await()
        pedidoDao.marcarComoSincronizado(pedido.id, pedido.statusEntrega, pedido.revisao)
        pedidoDao.receberPedido(atual)
    }
}

suspend fun acompanharProdutos(produtoDao: ProdutoDao) = coroutineScope {
    launch {
        while (isActive) {
            try {
                callbackFlow {
                    val registration = Firebase.firestore.collection("produtos")
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                            if (error != null) close(error)
                            else if (snapshot != null) trySend(snapshot.documents
                                .filter { !it.metadata.hasPendingWrites() }
                                .mapNotNull { doc -> doc.data?.let { produtoRemoto(doc.id, it) } })
                        }
                    awaitClose { registration.remove() }
                }.conflate().collect { produtos -> produtos.forEach { produtoDao.receberProduto(it) } }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.e("Sync", "Falha ao receber produtos.", e); delay(15_000) }
        }
    }
    launch {
        while (isActive) {
            try { sincronizarProdutos(produtoDao) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.e("Sync", "Produtos aguardando conexão.", e) }
            delay(15_000)
        }
    }
}

// O listener alimenta o Room, que continua sendo a fonte das listas na interface.
private fun observarPedidosRemotos() = callbackFlow {
    val registration = Firebase.firestore.collection("pedidos")
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                val pedidos = snapshot.documents
                    .filter { !it.metadata.hasPendingWrites() }
                    .mapNotNull { doc -> doc.data?.let { pedidoRemoto(doc.id, it) } }
                trySend(pedidos)
            }
        }
    awaitClose { registration.remove() }
}.conflate()

suspend fun acompanharPedidos(pedidoDao: PedidoDao) = coroutineScope {
    launch {
        while (isActive) {
            try {
                observarPedidosRemotos().collect { pedidos ->
                    pedidos.forEach { pedidoDao.receberPedido(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Sync", "Falha ao receber pedidos; nova tentativa em 15 segundos.", e)
                delay(15_000)
            }
        }
    }
    // Reenvia o cache local após falhas de rede; o mutex evita envios concorrentes.
    launch {
        while (isActive) {
            try {
                sincronizarPedidos(pedidoDao)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Sync", "Pedidos continuam no cache local para reenvio.", e)
            }
            delay(15_000)
        }
    }
}


suspend fun sincronizarAvaliacoes(avaliacaoDao: AvaliacaoDao) {
    val firestore = Firebase.firestore
    val pendentes = avaliacaoDao.buscarNaoSincronizados()

    pendentes.forEach { avaliacao ->
        firestore.collection("avaliacoes")
            .document(avaliacao.id.toString())
            .set(avaliacao)
            .await()

        avaliacaoDao.marcarComoSincronizado(avaliacao.id)
    }
}

suspend fun sincronizarTudo(db: AppDatabase) {
    runCatching { sincronizarUsuarios(db.usuarioDao()) }
        .onFailure { Log.e("Sync", "Falha ao sincronizar usuarios", it) }
    runCatching { sincronizarProdutos(db.produtoDao()) }
        .onFailure { Log.e("Sync", "Falha ao sincronizar produtos", it) }
    runCatching { sincronizarVeiculos(db.veiculoDao()) }
        .onFailure { Log.e("Sync", "Falha ao sincronizar veiculos", it) }
    runCatching { sincronizarPedidos(db.pedidoDao()) }
        .onFailure { Log.e("Sync", "Falha ao sincronizar pedidos", it) }
    runCatching { sincronizarAvaliacoes(db.avaliacaoDAO()) }
        .onFailure { Log.e("Sync", "Falha ao sincronizar avaliacoes", it) }
}
