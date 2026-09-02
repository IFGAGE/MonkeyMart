package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.example.marketplace.data.entity.AvaliacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoDao {
    @Insert
    suspend fun salvarAvaliacao(avaliacao: AvaliacaoEntity)

    @Query("SELECT * FROM tabela_avaliacao")
    fun buscarTodas(): Flow<List<AvaliacaoEntity>>

    @Query("SELECT * FROM tabela_avaliacao WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<AvaliacaoEntity>

    @Query("UPDATE tabela_avaliacao SET isSynced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Int)
}