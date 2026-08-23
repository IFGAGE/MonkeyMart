package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_veiculo")
data class VeiculoEntity(
    @PrimaryKey val placa: String,
    val emailEntregador: String,
    val modelo: String,
    val marca: String,
    val ano: String
)