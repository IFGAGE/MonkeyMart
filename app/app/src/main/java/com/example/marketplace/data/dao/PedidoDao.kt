package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.marketplace.data.sync.mesclarPedido
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

    @Query("UPDATE tabela_pedido SET isSynced = 1 WHERE id = :id AND statusEntrega = :statusEnviado")
    suspend fun marcarComoSincronizado(id: Int, statusEnviado: String)
}
