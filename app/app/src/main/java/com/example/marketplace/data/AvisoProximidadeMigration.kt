package com.example.marketplace.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_8_9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN avisoProximidade INTEGER NOT NULL DEFAULT 0")
    }
}
