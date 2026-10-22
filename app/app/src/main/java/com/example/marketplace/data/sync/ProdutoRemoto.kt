package com.example.marketplace.data.sync

import com.example.marketplace.data.entity.ProdutoEntity

fun produtoRemoto(documentId: String, dados: Map<String, Any?>): ProdutoEntity? {
    val vendedor = dados["emailNegociante"] as? String ?: return null
    val nome = dados["nome"] as? String ?: return null
    if (vendedor.isBlank() || nome.isBlank() || documentId.isBlank()) return null
    return ProdutoEntity(emailNegociante = vendedor,
        nomeNegociante = dados["nomeNegociante"] as? String ?: vendedor,
        nome = nome, preco = dados["preco"] as? String ?: "",
        descricao = dados["descricao"] as? String ?: "",
        // Arquivos locais do vendedor não existem no aparelho do comprador.
        fotoPathLocal = null, firestoreId = documentId, isSynced = true,
        enderecoColeta = dados["enderecoColeta"] as? String ?: "",
        coletaLatitude = (dados["coletaLatitude"] as? Number)?.toDouble(),
        coletaLongitude = (dados["coletaLongitude"] as? Number)?.toDouble(),
        revisao = (dados["revisao"] as? Number)?.toInt() ?: 0)
}

fun mesclarProduto(local: ProdutoEntity?, remoto: ProdutoEntity): ProdutoEntity = when {
    local == null -> remoto.copy(id = 0, isSynced = true)
    remoto.revisao < local.revisao || !local.isSynced && local.revisao >= remoto.revisao -> local
    else -> remoto.copy(id = local.id, fotoPathLocal = local.fotoPathLocal, isSynced = true)
}
