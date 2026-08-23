package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_pedido")
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailCliente: String, // Quem fez o pedido
    val enderecoEntrega: String, // Pra onde o entregador vai
    val resumoItens: String, // Ex: "2x Bolo, 1x Suco"
    val valorTotal: String, 
    val statusEntrega: String = "PENDENTE" // Começa como PENDENTE, o entregador muda pra ENTREGUE
)