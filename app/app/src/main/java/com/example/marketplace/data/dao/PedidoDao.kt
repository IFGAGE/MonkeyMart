package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.example.marketplace.data.entity.PedidoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    
    @Insert
    suspend fun salvarPedido(pedido: PedidoEntity)

    @Query("SELECT * FROM tabela_pedido WHERE statusEntrega != 'ENTREGUE'")
    fun buscarPedidosPendentes(): Flow<List<PedidoEntity>>

    // Por enquanto muda direto pra Entregue.
    // isSynced volta pra 0 pra essa mudança de status também subir pro Firestore.
    @Query("UPDATE tabela_pedido SET statusEntrega = :novoStatus, isSynced = 0 WHERE id = :pedidoId")
    suspend fun atualizarStatus(pedidoId: Int, novoStatus: String)

    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email ORDER BY id DESC")
    fun buscarPedidosDoCliente(email: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email AND statusEntrega = 'ENTREGUE'")
    fun buscarPedidosEntregues(email: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<PedidoEntity>

    @Query("UPDATE tabela_pedido SET isSynced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Int)
}