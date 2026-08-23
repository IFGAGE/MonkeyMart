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

    // O 'Flow' avisa a tela sempre que um produto novo é inserido no banco
    @Query("SELECT * FROM tabela_produto WHERE emailDono = :email")
    fun buscarProdutosDoNegociante(email: String): Flow<List<ProdutoEntity>>
    
    // Para o entregador ver todos os produtos disponíveis
    @Query("SELECT * FROM tabela_produto")
    fun buscarTodosProdutos(): Flow<List<ProdutoEntity>>
}