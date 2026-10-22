package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.marketplace.data.sync.mesclarPedido
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.EnderecoValidado
import com.example.marketplace.data.ItemCompra
import com.example.marketplace.data.criarPedidos
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    
    @Insert
    suspend fun salvarPedido(pedido: PedidoEntity)

    @Query("SELECT * FROM tabela_pedido WHERE statusEntrega = 'PRONTO_RETIRADA' AND emailEntregador = '' AND emailNegociante != '' AND enderecoColeta != '' AND coletaLatitude IS NOT NULL AND coletaLongitude IS NOT NULL AND entregaLatitude IS NOT NULL AND entregaLongitude IS NOT NULL")
    fun buscarPedidosPendentes(): Flow<List<PedidoEntity>>

    @Insert
    suspend fun salvarPedidos(pedidos: List<PedidoEntity>)

    @Query("SELECT * FROM tabela_produto WHERE id IN (:ids)")
    suspend fun produtosDaCompra(ids: List<Int>): List<ProdutoEntity>

    @Transaction
    suspend fun finalizarCompra(cliente: String, quantidades: Map<Int, Int>, entrega: EnderecoValidado) {
        val produtos = produtosDaCompra(quantidades.keys.toList())
        require(produtos.size == quantidades.size) { "Um produto não está mais disponível." }
        salvarPedidos(criarPedidos(cliente, produtos.map { ItemCompra(it, quantidades.getValue(it.id)) }, entrega))
    }

    @Query("SELECT * FROM tabela_pedido WHERE emailNegociante = :email ORDER BY id DESC")
    fun buscarPedidosDoVendedor(email: String): Flow<List<PedidoEntity>>

    @Query("UPDATE tabela_pedido SET statusEntrega = 'PRONTO_RETIRADA', revisao = revisao + 1, isSynced = 0 WHERE id = :id AND emailNegociante = :email AND statusEntrega = 'AGUARDANDO_PREPARO'")
    suspend fun marcarPronto(id: Int, email: String): Int

    // Por enquanto muda direto pra Entregue.
    // isSynced volta pra 0 pra essa mudança de status também subir pro Firestore.
    @Query("UPDATE tabela_pedido SET statusEntrega = :novoStatus, revisao = revisao + 1, isSynced = 0 WHERE id = :pedidoId")
    suspend fun atualizarStatus(pedidoId: Int, novoStatus: String)

    @Query("UPDATE tabela_pedido SET statusEntrega = :novoStatus, revisao = revisao + 1, isSynced = 0 WHERE id = :id AND emailEntregador = :email AND coletado = 1 AND statusEntrega IN ('EM_ROTA', 'ENTREGUE') AND :novoStatus IN ('EM_ROTA', 'ENTREGUE')")
    suspend fun atualizarEntrega(id: Int, email: String, novoStatus: String): Int

    @Query("UPDATE tabela_pedido SET coletado = :coletado, revisao = revisao + 1, isSynced = 0 WHERE id IN (:pedidoIds) AND emailEntregador = :email AND statusEntrega = 'EM_ROTA' AND coletado != :coletado")
    suspend fun gravarColetas(pedidoIds: List<Int>, email: String, coletado: Boolean): Int

    @Query("SELECT * FROM tabela_pedido WHERE id IN (:pedidoIds)")
    suspend fun carregarPedidos(pedidoIds: List<Int>): List<PedidoEntity>

    @Transaction
    suspend fun atualizarColetas(pedidoIds: List<Int>, email: String, coletado: Boolean) {
        val pedidos = carregarPedidos(pedidoIds)
        check(pedidos.size == pedidoIds.distinct().size && pedidos.isNotEmpty()) { "Aguarde o carregamento dos pedidos." }
        check(pedidos.all { it.emailEntregador == email && it.statusEntrega in listOf("EM_ROTA", "ENTREGUE") }) {
            "Esses pedidos não pertencem à sua rota."
        }
        check(coletado || pedidos.none { it.statusEntrega == "ENTREGUE" }) { "Desmarque a entrega antes de desmarcar a coleta." }
        val esperados = pedidos.count { it.coletado != coletado }
        check(gravarColetas(pedidoIds, email, coletado) == esperados) { "Não foi possível atualizar a coleta." }
    }

    @Transaction
    suspend fun atualizarEntregas(pedidoIds: List<Int>, email: String, entregue: Boolean) {
        val pedidos = carregarPedidos(pedidoIds)
        check(pedidos.size == pedidoIds.distinct().size && pedidos.isNotEmpty()) { "Aguarde o carregamento dos pedidos." }
        check(pedidos.all { it.emailEntregador == email && it.coletado && it.statusEntrega in listOf("EM_ROTA", "ENTREGUE") }) {
            "Confirme a coleta de todos os pedidos desta entrega primeiro."
        }
        val status = if (entregue) "ENTREGUE" else "EM_ROTA"
        pedidos.filter { it.statusEntrega != status }.forEach {
            check(atualizarEntrega(it.id, email, status) == 1) { "Não foi possível atualizar a entrega." }
        }
    }

    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email ORDER BY id DESC")
    fun buscarPedidosDoCliente(email: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email AND statusEntrega = 'ENTREGUE'")
    fun buscarPedidosEntregues(email: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE id IN (:pedidoIds)")
    fun buscarPorIds(pedidoIds: List<Int>): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun buscarPorFirestoreId(firestoreId: String): PedidoEntity?

    @Update
    suspend fun atualizarPedido(pedido: PedidoEntity)

    @Transaction
    suspend fun receberPedido(remoto: PedidoEntity) {
        val local = buscarPorFirestoreId(remoto.firestoreId)
        val mesclado = mesclarPedido(local, remoto)
        if (local == null) salvarPedido(mesclado)
        else if (mesclado != local) atualizarPedido(mesclado)
    }

    @Query("SELECT * FROM tabela_pedido WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<PedidoEntity>

    @Query("UPDATE tabela_pedido SET isSynced = 1 WHERE id = :id AND statusEntrega = :statusEnviado AND revisao = :revisao")
    suspend fun marcarComoSincronizado(id: Int, statusEnviado: String, revisao: Int = 0)
}
