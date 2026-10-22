package com.example.marketplace.data.entity

data class EnderecoValidado(val endereco: String, val latitude: Double, val longitude: Double) {
    fun valido() = endereco.isNotBlank() && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}
