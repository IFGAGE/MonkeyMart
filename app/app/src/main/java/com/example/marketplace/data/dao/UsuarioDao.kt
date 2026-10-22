package com.example.marketplace.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import com.example.marketplace.data.entity.UsuarioEntity // <-- Import da Entity que você acabou de criar

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarUsuario(usuario: UsuarioEntity)

    @Query("UPDATE tabela_produto SET enderecoColeta = :endereco, coletaLatitude = :latitude, coletaLongitude = :longitude, revisao = revisao + 1, isSynced = 0 WHERE emailNegociante = :email")
    suspend fun atualizarColetaProdutos(email: String, endereco: String, latitude: Double, longitude: Double)

    @Transaction
    suspend fun salvarColeta(usuario: UsuarioEntity) {
        val endereco = requireNotNull(usuario.enderecoColeta?.validado())
        salvarUsuario(usuario.copy(isSynced = false))
        atualizarColetaProdutos(usuario.email, endereco.endereco, endereco.latitude, endereco.longitude)
    }

    @Query("SELECT * FROM tabela_usuario WHERE email = :email LIMIT 1")
    suspend fun buscarUsuario(email: String): UsuarioEntity?

    @Query("SELECT * FROM tabela_usuario WHERE email = :email LIMIT 1")
    fun observarUsuario(email: String): Flow<UsuarioEntity?>

    @Query("SELECT * FROM tabela_usuario WHERE isSynced = 0")
    suspend fun buscarNaoSincronizados(): List<UsuarioEntity>

    @Query("UPDATE tabela_usuario SET isSynced = 1 WHERE email = :email")
    suspend fun marcarComoSincronizado(email: String)

    //@Query("DELETE FROM tabela_usuario")
    //suspend fun deletarTudo()
}
