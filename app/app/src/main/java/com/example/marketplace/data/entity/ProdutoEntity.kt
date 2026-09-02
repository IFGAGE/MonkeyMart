package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_produto")
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailNegociante: String,
    val nomeNegociante: String,
    val nome: String,
    val preco: String,
    val descricao: String,
    val fotoPathLocal: String? = null,
    val isSynced: Boolean = false
)