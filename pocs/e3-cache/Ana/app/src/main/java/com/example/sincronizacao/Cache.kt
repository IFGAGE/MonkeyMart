package com.example.sincronizacao

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val preco: Double,
    val isSynced: Boolean = false
)

@Dao
interface ProdutoDao {
    @Query("SELECT * FROM produtos ORDER BY nome")
    fun listarTodos(): Flow<List<Produto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(produto: Produto)

    @Query("SELECT * FROM produtos WHERE isSynced = 0")
    suspend fun listarPendentes(): List<Produto>

    @Query("UPDATE produtos SET isSynced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String)
}

@Database(entities = [Produto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun produtoDao(): ProdutoDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cache.db"
                ).build().also { instance = it }
            }
    }
}
