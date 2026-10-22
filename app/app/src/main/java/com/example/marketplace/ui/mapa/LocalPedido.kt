package com.example.marketplace.ui.mapa

import com.example.marketplace.data.entity.EnderecoValidado
import com.example.marketplace.ui.screens.entregador.PedidoTemp

data class LocalPedido(val pedidoId: Int, val tipo: TipoParada, val endereco: String, val coordenadas: Coordenadas?)

fun PedidoTemp.localNoMapa(tipo: TipoParada): LocalPedido {
    val coleta = tipo == TipoParada.COLETA
    val endereco = if (coleta) enderecoColeta else enderecoEntrega
    val latitude = if (coleta) coletaLatitude else entregaLatitude
    val longitude = if (coleta) coletaLongitude else entregaLongitude
    val ponto = if (latitude != null && longitude != null && EnderecoValidado(endereco, latitude, longitude).valido())
        Coordenadas(latitude, longitude) else null
    return LocalPedido(id, tipo, endereco, ponto)
}
