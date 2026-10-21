package com.example.marketplace.data.entity

fun descricaoStatusCliente(status: String): String = when (status) {
    "PRONTO_RETIRADA" -> "Aguardando entregador"
    "EM_ROTA" -> "Saiu para entrega"
    else -> descricaoStatus(status)
}

fun descricaoStatus(status: String): String = when (status) {
    "AGUARDANDO_PREPARO" -> "Aguardando preparo"
    "PRONTO_RETIRADA" -> "Pronto para retirada"
    "EM_ROTA" -> "Em rota"
    "ENTREGUE" -> "Entregue"
    "PENDENTE" -> "Pendente"
    else -> status
}
