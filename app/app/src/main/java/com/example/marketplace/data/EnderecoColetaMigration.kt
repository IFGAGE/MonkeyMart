package com.example.marketplace.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        listOf("cep", "rua", "numero", "complemento", "bairro", "cidade", "uf").forEach { campo ->
            connection.execSQL("ALTER TABLE tabela_usuario ADD COLUMN coleta_$campo TEXT")
        }
    }
}
