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
        isSynced = true, firestoreId = documentId
    )
}

fun dadosPedido(pedido: PedidoEntity): Map<String, Any> = mapOf(
    "emailCliente" to pedido.emailCliente,
    "enderecoEntrega" to pedido.enderecoEntrega,
    "resumoItens" to pedido.resumoItens,
    "valorTotal" to pedido.valorTotal,
    "statusEntrega" to pedido.statusEntrega,
    "firestoreId" to pedido.firestoreId
)

fun mesclarPedido(local: PedidoEntity?, remoto: PedidoEntity): PedidoEntity = when {
    local == null -> remoto.copy(id = 0, isSynced = true)
    !local.isSynced -> local // Uma alteração offline ainda precisa ser enviada.
    else -> remoto.copy(id = local.id, isSynced = true)
}
