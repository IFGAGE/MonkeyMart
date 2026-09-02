package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_pedido")
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String = "PENDENTE",
    val isSynced: Boolean = false
)