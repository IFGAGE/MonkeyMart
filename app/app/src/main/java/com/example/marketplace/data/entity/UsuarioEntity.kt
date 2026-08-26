package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_usuario")
data class UsuarioEntity(
    @PrimaryKey val email: String,
    val nome: String,
    val telefone: String,
    val cpf: String,
    val dataNascimento: String,
    val tipoPerfil: String
)