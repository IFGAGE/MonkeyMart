package com.example.cadastros.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cadastros.data.local.entity.Produto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    @Query("SELECT * FROM produtos ORDER BY id DESC")
    fun listarTodos(): Flow<List<Produto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirProduto(produto: Produto)

    @Query("SELECT * FROM produtos WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<Produto>

    @Query("UPDATE produtos SET isSynced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String)
}