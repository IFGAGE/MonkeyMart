package com.example.marketplace.data.entity

data class EnderecoColeta(
    val cep: String = "",
    val rua: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val endereco: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun texto(): String = endereco.ifBlank {
        listOf(rua, numero, complemento, bairro, cidade, uf, cep).filter { it.isNotBlank() }.joinToString(", ")
    }
    fun validado(): EnderecoValidado? = latitude?.let { lat -> longitude?.let { lon ->
        EnderecoValidado(endereco, lat, lon).takeIf { it.valido() }
    } }
    fun valido(): Boolean = validado() != null
}
