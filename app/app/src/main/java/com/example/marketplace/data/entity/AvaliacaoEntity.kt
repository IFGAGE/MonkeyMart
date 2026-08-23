package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_avaliacao")
data class AvaliacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomeProduto: String,
    val emailAutor: String,
    val nota: Int, // 1 a 5
    val comentario: String
)