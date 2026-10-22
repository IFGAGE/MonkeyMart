package com.example.marketplace.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tabela_usuario ADD COLUMN coleta_endereco TEXT")
        connection.execSQL("ALTER TABLE tabela_usuario ADD COLUMN coleta_latitude REAL")
        connection.execSQL("ALTER TABLE tabela_usuario ADD COLUMN coleta_longitude REAL")
        connection.execSQL("ALTER TABLE tabela_produto ADD COLUMN enderecoColeta TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE tabela_produto ADD COLUMN coletaLatitude REAL")
        connection.execSQL("ALTER TABLE tabela_produto ADD COLUMN coletaLongitude REAL")
        connection.execSQL("ALTER TABLE tabela_produto ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE tabela_produto ADD COLUMN revisao INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE tabela_produto SET firestoreId = CAST(id AS TEXT)")
        connection.execSQL("CREATE UNIQUE INDEX index_tabela_produto_firestoreId ON tabela_produto(firestoreId)")
        listOf("emailNegociante", "enderecoColeta", "emailEntregador").forEach {
            connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN $it TEXT NOT NULL DEFAULT ''")
        }
        listOf("coletaLatitude", "coletaLongitude", "entregaLatitude", "entregaLongitude").forEach {
            connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN $it REAL")
        }
        connection.execSQL("ALTER TABLE tabela_pedido ADD COLUMN revisao INTEGER NOT NULL DEFAULT 0")
    }
}
