package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_produto")
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailDono: String,
    val nomeDono: String, // <-- NOVO: Salva o nome de quem tá vendendo
    val nome: String,
    val preco: String,
    val descricao: String,
    val fotoPathLocal: String? = null // <-- NOVO: Preparado para a foto da Semana 9
)