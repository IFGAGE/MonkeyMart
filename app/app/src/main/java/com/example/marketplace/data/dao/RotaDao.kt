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

    @Query("UPDATE tabela_rota SET placaVeiculo = :placa WHERE emailEntregador = :email AND estado = 'PLANEJADA' AND pedidosJson = :pedidosAnteriores AND origemJson = :origemAnterior AND destinosJson = :destinosAnteriores AND geometriaJson = :geometriaAnterior")
    protected abstract suspend fun gravarVeiculo(email: String, placa: String, pedidosAnteriores: String,
        origemAnterior: String, destinosAnteriores: String, geometriaAnterior: String): Int

    @Transaction
    open suspend fun selecionarVeiculo(rota: RotaEntity, placa: String) {
        check(contarVeiculo(rota.emailEntregador, placa) == 1) { "Veículo não encontrado." }
        check(gravarVeiculo(rota.emailEntregador, placa, rota.pedidosJson, rota.origemJson,
            rota.destinosJson, rota.geometriaJson) == 1) { "A rota mudou. Abra-a novamente." }
    }

    @Query("UPDATE tabela_rota SET pedidosJson = :pedidosJson, origemJson = :origemJson, destinosJson = :destinosJson, geometriaJson = :geometriaJson WHERE emailEntregador = :email AND estado = 'PLANEJADA' AND pedidosJson = :pedidosAnteriores AND origemJson = :origemAnterior AND destinosJson = :destinosAnteriores AND geometriaJson = :geometriaAnterior")
    abstract suspend fun atualizarPlanejada(email: String, pedidosJson: String, origemJson: String,
        destinosJson: String, geometriaJson: String, pedidosAnteriores: String, origemAnterior: String,
        destinosAnteriores: String, geometriaAnterior: String): Int

    @Query("UPDATE tabela_rota SET estado = 'INICIADA', placaVeiculo = :placa WHERE emailEntregador = :email AND estado IN ('PLANEJADA', 'RESERVANDO')")
    protected abstract suspend fun marcarIniciada(email: String, placa: String): Int

    @Query("UPDATE tabela_rota SET estado = 'RESERVANDO', placaVeiculo = :placa WHERE emailEntregador = :email AND estado IN ('PLANEJADA', 'RESERVANDO')")
    abstract suspend fun marcarReservando(email: String, placa: String): Int

    @Query("DELETE FROM tabela_rota WHERE emailEntregador = :email AND estado = 'RESERVANDO'")
    abstract suspend fun excluirReservaResolvida(email: String): Int

    @Query("UPDATE tabela_pedido SET statusEntrega = :status, revisao = revisao + 1, emailEntregador = '', isSynced = 0 WHERE id IN (:pedidoIds) AND statusEntrega = :statusAtual AND emailEntregador = :email")
    protected abstract suspend fun atualizarPedidos(
        pedidoIds: List<Int>,
        statusAtual: String,
        status: String,
        email: String
    ): Int

    @Query("UPDATE tabela_pedido SET revisao = CASE WHEN statusEntrega = 'PRONTO_RETIRADA' THEN revisao + 1 ELSE revisao END, statusEntrega = 'EM_ROTA', emailEntregador = :email, isSynced = 0 WHERE id IN (:pedidoIds) AND ((statusEntrega = 'PRONTO_RETIRADA' AND emailEntregador = '') OR (statusEntrega = 'EM_ROTA' AND emailEntregador = :email))")
    protected abstract suspend fun assumirPedidos(pedidoIds: List<Int>, email: String): Int

    @Query("SELECT COUNT(*) FROM tabela_pedido WHERE id IN (:pedidoIds) AND (statusEntrega != 'ENTREGUE' OR coletado = 0)")
    protected abstract suspend fun contarNaoEntregues(pedidoIds: List<Int>): Int

    @Query("SELECT COUNT(*) FROM tabela_pedido WHERE id IN (:pedidoIds) AND (coletado = 1 OR statusEntrega = 'ENTREGUE')")
    protected abstract suspend fun contarColetados(pedidoIds: List<Int>): Int

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
        check(assumirPedidos(pedidoIds, email) == pedidoIds.size) {
            "Um ou mais pedidos não estão disponíveis."
        }
    }

    @Transaction
    open suspend fun cancelar(email: String, pedidoIds: List<Int>) {
        check(contarColetados(pedidoIds) == 0) { "Após uma coleta, conclua as entregas para encerrar a rota." }
        atualizarPedidos(pedidoIds, "EM_ROTA", "PRONTO_RETIRADA", email)
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
