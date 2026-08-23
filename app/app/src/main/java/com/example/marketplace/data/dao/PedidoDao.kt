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

    // O comando mágico pro Entregador apertar o botão e mudar o status na hora!
    @Query("UPDATE tabela_pedido SET statusEntrega = :novoStatus WHERE id = :pedidoId")
    suspend fun atualizarStatus(pedidoId: Int, novoStatus: String)

    // NOVO: Busca todos os pedidos que o usuário logado fez, do mais novo pro mais antigo
    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email ORDER BY id DESC")
    fun buscarPedidosDoCliente(email: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM tabela_pedido WHERE emailCliente = :email AND statusEntrega = 'ENTREGUE'")
    fun buscarPedidosEntregues(email: String): Flow<List<PedidoEntity>>
}