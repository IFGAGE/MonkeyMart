package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.marketplace.data.entity.ProdutoEntity
import kotlinx.coroutines.flow.Flow // Vamos usar Flow para a lista atualizar sozinha na tela!

@Dao
interface ProdutoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarProduto(produto: ProdutoEntity)

    @Query("SELECT * FROM tabela_produto WHERE emailNegociante = :email")
    fun buscarProdutosDoNegociante(email: String): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM tabela_produto")
    fun buscarTodosProdutos(): Flow<List<ProdutoEntity>>
}