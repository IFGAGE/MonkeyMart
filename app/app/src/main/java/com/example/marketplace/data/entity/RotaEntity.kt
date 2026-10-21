package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_rota")
data class RotaEntity(
    @PrimaryKey val emailEntregador: String,
    val estado: String = "PLANEJADA",
    val placaVeiculo: String? = null,
    val pedidosJson: String,
    val origemJson: String,
    val destinosJson: String,
    val geometriaJson: String
)
