package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.marketplace.data.sync.mesclarProduto
import com.example.marketplace.data.entity.ProdutoEntity
import kotlinx.coroutines.flow.Flow // Vamos usar Flow para a lista atualizar sozinha na tela!

@Dao
interface ProdutoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarProduto(produto: ProdutoEntity)

    @Query("SELECT * FROM tabela_produto WHERE id IN (:ids)")
    suspend fun buscarPorIds(ids: List<Int>): List<ProdutoEntity>

    @Query("SELECT * FROM tabela_produto WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun buscarPorFirestoreId(firestoreId: String): ProdutoEntity?

    @Transaction
    suspend fun receberProduto(remoto: ProdutoEntity) {
        val local = buscarPorFirestoreId(remoto.firestoreId)
        val mesclado = mesclarProduto(local, remoto)
        if (mesclado != local) salvarProduto(mesclado)
    }

    @Transaction
    suspend fun confirmarEnvio(produto: ProdutoEntity) {
        if (buscarPorFirestoreId(produto.firestoreId) == produto) marcarComoSincronizado(produto.id)
    }

    @Query("SELECT * FROM tabela_produto WHERE emailNegociante = :email")
    fun buscarProdutosDoNegociante(email: String): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM tabela_produto")
    fun buscarTodosProdutos(): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM tabela_produto WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<ProdutoEntity>

    @Query("UPDATE tabela_produto SET isSynced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Int)
}
