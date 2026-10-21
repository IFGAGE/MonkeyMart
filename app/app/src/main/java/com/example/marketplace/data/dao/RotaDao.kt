package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.marketplace.data.entity.RotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RotaDao {
    @Query("SELECT * FROM tabela_rota WHERE emailEntregador = :email LIMIT 1")
    abstract fun observar(email: String): Flow<RotaEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun salvar(rota: RotaEntity)

    @Query("UPDATE tabela_rota SET estado = 'INICIADA', placaVeiculo = :placa WHERE emailEntregador = :email AND estado = 'PLANEJADA'")
    protected abstract suspend fun marcarIniciada(email: String, placa: String): Int

    @Query("UPDATE tabela_pedido SET statusEntrega = :status, isSynced = 0 WHERE id IN (:pedidoIds) AND statusEntrega = :statusAtual")
    protected abstract suspend fun atualizarPedidos(
        pedidoIds: List<Int>,
        statusAtual: String,
        status: String
    ): Int

    @Query("SELECT COUNT(*) FROM tabela_pedido WHERE id IN (:pedidoIds) AND statusEntrega != 'ENTREGUE'")
    protected abstract suspend fun contarNaoEntregues(pedidoIds: List<Int>): Int

    @Query("SELECT COUNT(*) FROM tabela_pedido WHERE id IN (:pedidoIds)")
    protected abstract suspend fun contarPedidos(pedidoIds: List<Int>): Int

    @Query("SELECT COUNT(*) FROM tabela_veiculo WHERE emailEntregador = :email AND placa = :placa")
    protected abstract suspend fun contarVeiculo(email: String, placa: String): Int

    @Query("DELETE FROM tabela_rota WHERE emailEntregador = :email AND estado = :estado")
    protected abstract suspend fun excluirPorEstado(email: String, estado: String): Int

    @Transaction
    open suspend fun iniciar(email: String, placa: String, pedidoIds: List<Int>) {
        require(pedidoIds.isNotEmpty()) { "A rota precisa ter pedidos." }
        require(placa.isNotBlank()) { "Selecione um veículo." }
        check(contarVeiculo(email, placa) == 1) { "Veículo não encontrado." }
        check(marcarIniciada(email, placa) == 1) { "Rota planejada não encontrada." }
        check(atualizarPedidos(pedidoIds, "PENDENTE", "EM_ROTA") == pedidoIds.size) {
            "Um ou mais pedidos não estão disponíveis."
        }
    }

    @Transaction
    open suspend fun cancelar(email: String, pedidoIds: List<Int>) {
        atualizarPedidos(pedidoIds, "EM_ROTA", "PENDENTE")
        check(excluirPorEstado(email, "INICIADA") == 1) { "Rota iniciada não encontrada." }
    }

    @Transaction
    open suspend fun finalizar(email: String, pedidoIds: List<Int>) {
        check(contarPedidos(pedidoIds) == pedidoIds.size) { "Um ou mais pedidos não foram encontrados." }
        check(contarNaoEntregues(pedidoIds) == 0) { "Ainda existem pedidos não entregues." }
        check(excluirPorEstado(email, "INICIADA") == 1) { "Rota iniciada não encontrada." }
    }

    @Transaction
    open suspend fun excluirPlanejada(email: String) {
        check(excluirPorEstado(email, "PLANEJADA") == 1) { "Rota planejada não encontrada." }
    }
}
