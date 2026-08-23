package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.marketplace.data.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarVeiculo(veiculo: VeiculoEntity)

    @Query("SELECT * FROM tabela_veiculo WHERE emailEntregador = :email")
    fun buscarVeiculosDoEntregador(email: String): Flow<List<VeiculoEntity>>
}