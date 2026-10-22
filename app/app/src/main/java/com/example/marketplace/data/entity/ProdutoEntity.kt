package com.example.marketplace.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.ColumnInfo
import androidx.room3.Index
import java.util.UUID

@Entity(tableName = "tabela_produto", indices = [Index(value = ["firestoreId"], unique = true)])
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailNegociante: String,
    val nomeNegociante: String,
    val nome: String,
    val preco: String,
    val descricao: String,
    val fotoPathLocal: String? = null,
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "''") val enderecoColeta: String = "",
    val coletaLatitude: Double? = null,
    val coletaLongitude: Double? = null,
    @ColumnInfo(defaultValue = "''") val firestoreId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val revisao: Int = 0
) {
    fun podeSerCompradoPor(email: String): Boolean = email.isNotBlank() && emailNegociante != email
    fun coletaValidada(): EnderecoValidado? = coletaLatitude?.let { lat -> coletaLongitude?.let { lon ->
        EnderecoValidado(enderecoColeta, lat, lon).takeIf { it.valido() }
    } }
}
