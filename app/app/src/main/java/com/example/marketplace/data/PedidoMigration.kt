package com.example.marketplace.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
        // Mantém o vínculo com os documentos numéricos das versões anteriores.
        connection.execSQL("UPDATE tabela_pedido SET firestoreId = CAST(id AS TEXT)")
        connection.execSQL("CREATE UNIQUE INDEX index_tabela_pedido_firestoreId ON tabela_pedido(firestoreId)")
    }
}
