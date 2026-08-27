package com.example.sincronizacao.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cadastros.data.local.entity.Produto
import com.example.cadastros.data.local.dao.ProdutoDao

@Database(entities = [Produto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao

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