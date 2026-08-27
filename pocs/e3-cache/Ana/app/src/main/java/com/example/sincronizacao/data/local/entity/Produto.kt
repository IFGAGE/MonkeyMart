package com.example.cadastros.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String,
    val preco: Double,
    val descricao: String,
    val isSynced: Boolean = false
)