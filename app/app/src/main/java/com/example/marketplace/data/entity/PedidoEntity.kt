package com.example.marketplace.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Index
import java.util.UUID
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tabela_pedido", indices = [Index(value = ["firestoreId"], unique = true)])
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String = "PENDENTE",
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "''") val firestoreId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "''") val emailNegociante: String = "",
    @ColumnInfo(defaultValue = "''") val enderecoColeta: String = "",
    val coletaLatitude: Double? = null,
    val coletaLongitude: Double? = null,
    val entregaLatitude: Double? = null,
    val entregaLongitude: Double? = null,
    @ColumnInfo(defaultValue = "''") val emailEntregador: String = "",
    @ColumnInfo(defaultValue = "0") val revisao: Int = 0,
    @ColumnInfo(defaultValue = "0") val coletado: Boolean = false
)
