package com.example.marketplace.data.sync

import com.example.marketplace.data.entity.PedidoEntity

// Não usa o ID numérico remoto como chave local: cada dispositivo tem sua sequência.
fun pedidoRemoto(documentId: String, dados: Map<String, Any?>): PedidoEntity? {
    val email = dados["emailCliente"] as? String ?: return null
    val endereco = dados["enderecoEntrega"] as? String ?: return null
    val resumo = dados["resumoItens"] as? String ?: return null
    val total = dados["valorTotal"] as? String ?: return null
    if (documentId.isBlank() || email.isBlank() || endereco.isBlank()) return null
    return PedidoEntity(
        emailCliente = email, enderecoEntrega = endereco, resumoItens = resumo,
        valorTotal = total, statusEntrega = dados["statusEntrega"] as? String ?: "PENDENTE",
        isSynced = true, firestoreId = documentId,
        emailNegociante = dados["emailNegociante"] as? String ?: "",
        enderecoColeta = dados["enderecoColeta"] as? String ?: "",
        coletaLatitude = (dados["coletaLatitude"] as? Number)?.toDouble(),
        coletaLongitude = (dados["coletaLongitude"] as? Number)?.toDouble(),
        entregaLatitude = (dados["entregaLatitude"] as? Number)?.toDouble(),
        entregaLongitude = (dados["entregaLongitude"] as? Number)?.toDouble(),
        emailEntregador = dados["emailEntregador"] as? String ?: "",
        revisao = (dados["revisao"] as? Number)?.toInt() ?: 0,
        coletado = dados["coletado"] as? Boolean ?: (dados["statusEntrega"] == "ENTREGUE")
    )
}

fun dadosPedido(pedido: PedidoEntity): Map<String, Any?> = mapOf(
    "emailCliente" to pedido.emailCliente,
    "enderecoEntrega" to pedido.enderecoEntrega,
    "resumoItens" to pedido.resumoItens,
    "valorTotal" to pedido.valorTotal,
    "statusEntrega" to pedido.statusEntrega,
    "firestoreId" to pedido.firestoreId,
    "emailNegociante" to pedido.emailNegociante,
    "enderecoColeta" to pedido.enderecoColeta,
    "coletaLatitude" to pedido.coletaLatitude,
    "coletaLongitude" to pedido.coletaLongitude,
    "entregaLatitude" to pedido.entregaLatitude,
    "entregaLongitude" to pedido.entregaLongitude,
    "emailEntregador" to pedido.emailEntregador,
    "revisao" to pedido.revisao,
    "coletado" to pedido.coletado
)

fun mesclarPedido(local: PedidoEntity?, remoto: PedidoEntity): PedidoEntity = when {
    local == null -> remoto.copy(id = 0, isSynced = true)
    remoto.revisao < local.revisao -> local // Listeners podem entregar snapshots anteriores à reserva.
    !local.isSynced && local.revisao >= remoto.revisao -> local
    else -> remoto.copy(id = local.id, isSynced = true)
}
