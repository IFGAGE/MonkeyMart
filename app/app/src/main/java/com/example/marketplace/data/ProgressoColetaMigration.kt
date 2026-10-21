package com.example.marketplace.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN coletado INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE tabela_pedido SET coletado = 1 WHERE statusEntrega = 'ENTREGUE'")
    }
}
