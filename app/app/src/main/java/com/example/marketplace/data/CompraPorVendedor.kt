package com.example.marketplace.data

import com.example.marketplace.data.entity.*
import java.math.BigDecimal
import java.math.RoundingMode

data class ItemCompra(val produto: ProdutoEntity, val quantidade: Int)

fun criarPedidos(emailCliente: String, itens: List<ItemCompra>, entrega: EnderecoValidado): List<PedidoEntity> {
    require(emailCliente.isNotBlank() && entrega.valido() && itens.isNotEmpty()) { "Compra ou endereço inválido." }
    itens.forEach {
        require(it.quantidade > 0 && it.produto.emailNegociante.isNotBlank() && it.produto.podeSerCompradoPor(emailCliente)) {
            "Não é possível comprar esses produtos."
        }
        require(it.produto.coletaValidada() != null) { "O vendedor ${it.produto.nomeNegociante} precisa validar o endereço de coleta." }
    }
    return itens.groupBy { it.produto.emailNegociante }.map { (vendedor, grupo) ->
        val coleta = grupo.first().produto.coletaValidada()!!
        require(grupo.all { it.produto.coletaValidada() == coleta }) { "O vendedor precisa atualizar a coleta de seus produtos." }
        val total = grupo.fold(BigDecimal.ZERO) { soma, item ->
            val preco = item.produto.preco.replace(",", ".").toBigDecimalOrNull()
            require(preco != null && preco > BigDecimal.ZERO) { "Preço de produto inválido." }
            soma + preco * item.quantidade.toBigDecimal()
        }.setScale(2, RoundingMode.HALF_UP).toPlainString()
        PedidoEntity(emailCliente = emailCliente, emailNegociante = vendedor,
            enderecoEntrega = entrega.endereco, entregaLatitude = entrega.latitude, entregaLongitude = entrega.longitude,
            enderecoColeta = coleta.endereco, coletaLatitude = coleta.latitude, coletaLongitude = coleta.longitude,
            resumoItens = grupo.joinToString { "${it.quantidade}x ${it.produto.nome}" }, valorTotal = total,
            statusEntrega = "AGUARDANDO_PREPARO")
    }
}
