package com.example.marketplace.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Index
import java.util.UUID
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_pedido", indices = [Index(value = ["firestoreId"], unique = true)])
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String = "PENDENTE",
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "''") val firestoreId: String = UUID.randomUUID().toString()
)
