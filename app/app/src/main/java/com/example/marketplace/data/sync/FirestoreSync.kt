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


suspend fun sincronizarProdutos(produtoDao: ProdutoDao) {
    val firestore = Firebase.firestore
    val produtosPendentes = produtoDao.buscarNaoSincronizados()

    produtosPendentes.forEach { produto ->
        firestore.collection("produtos")
            .document(produto.id.toString())
            .set(produto)
            .await()

        produtoDao.marcarComoSincronizado(produto.id)
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


suspend fun sincronizarPedidos(pedidoDao: PedidoDao) {
    val firestore = Firebase.firestore
    val pendentes = pedidoDao.buscarNaoSincronizados()

    pendentes.forEach { pedido ->
        firestore.collection("pedidos")
            .document(pedido.id.toString())
            .set(pedido)
            .await()

        pedidoDao.marcarComoSincronizado(pedido.id)
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
