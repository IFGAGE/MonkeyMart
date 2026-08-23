package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_veiculo")
data class VeiculoEntity(
    @PrimaryKey val placa: String, // A placa é única, serve perfeitamente como chave primária
    val emailDono: String, // Para sabermos de qual entregador é o veículo
    val modelo: String,
    val marca: String,
    val ano: String
)