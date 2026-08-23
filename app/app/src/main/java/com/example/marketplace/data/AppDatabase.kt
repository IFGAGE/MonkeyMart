package com.example.marketplace.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.example.marketplace.data.dao.ProdutoDao
import com.example.marketplace.data.dao.UsuarioDao
import com.example.marketplace.data.dao.VeiculoDao
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.UsuarioEntity
import com.example.marketplace.data.entity.VeiculoEntity

@Database(
    entities = [
        UsuarioEntity::class,
        ProdutoEntity::class,
        VeiculoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun produtoDao(): ProdutoDao
    abstract fun veiculoDao(): VeiculoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketplace_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}