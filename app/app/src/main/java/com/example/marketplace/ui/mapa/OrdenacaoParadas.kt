package com.example.marketplace.ui.mapa

enum class TipoParada { COLETA, ENTREGA }

// Índices retornados referem-se a paradas; a linha/coluna zero da matriz é a origem.
fun ordenarParadasComColeta(matriz: List<List<Double?>>, paradas: List<ParadaRota>): List<Int> {
    require(matriz.size == paradas.size + 1 && matriz.all { it.size == matriz.size }) { "Matriz de rota inválida." }
    if (paradas.isEmpty()) return emptyList()
    return if (paradas.size <= 12) ordenarRotaExata(matriz, paradas) else ordenarRotaGrande(matriz, paradas)
}

// Caminho de menor duração na matriz, respeitando todas as coletas antes de cada entrega.
private fun ordenarRotaExata(matriz: List<List<Double?>>, paradas: List<ParadaRota>): List<Int> {
    val n = paradas.size
    val requisitos = paradas.map { entrega ->
        if (entrega.tipo == TipoParada.COLETA) 0 else paradas.indices.fold(0) { bits, indice ->
            val coleta = paradas[indice]
            if (coleta.tipo == TipoParada.COLETA && coleta.pedidoIds.any { it in entrega.pedidoIds }) bits or (1 shl indice)
            else bits
        }
    }
    val estados = 1 shl n
    val largura = n + 1
    val custos = DoubleArray(estados * largura) { Double.POSITIVE_INFINITY }
    val anterior = IntArray(custos.size) { -1 }
    custos[n] = 0.0 // Estado vazio, último ponto = origem.
    for (mask in 0 until estados) {
        for (ultimo in 0..n) {
            val custo = custos[mask * largura + ultimo]
            if (!custo.isFinite()) continue
            for (proximo in 0 until n) {
                if (mask and (1 shl proximo) != 0 || mask and requisitos[proximo] != requisitos[proximo]) continue
                val trecho = matriz[if (ultimo == n) 0 else ultimo + 1][proximo + 1] ?: continue
                if (!trecho.isFinite() || trecho < 0) continue
                val destino = (mask or (1 shl proximo)) * largura + proximo
                if (custo + trecho < custos[destino]) {
                    custos[destino] = custo + trecho
                    anterior[destino] = ultimo
                }
            }
        }
    }
    var mask = estados - 1
    var ultimo = (0 until n).minByOrNull { custos[mask * largura + it] }!!
    require(custos[mask * largura + ultimo].isFinite()) { "Não há rota entre os endereços selecionados." }
    val ordem = mutableListOf<Int>()
    while (mask != 0) {
        ordem += ultimo
        val origem = anterior[mask * largura + ultimo]
        mask = mask xor (1 shl ultimo)
        ultimo = origem
    }
    return ordem.reversed()
}

// Limita memória e tempo para seleções grandes; mantém as mesmas regras de precedência.
private fun ordenarRotaGrande(matriz: List<List<Double?>>, paradas: List<ParadaRota>): List<Int> {
    val restantes = paradas.indices.toMutableSet()
    val coletados = mutableSetOf<Int>()
    val pedidosComColeta = paradas.filter { it.tipo == TipoParada.COLETA }.flatMap { it.pedidoIds }.toSet()
    val ordem = mutableListOf<Int>()
    var atual = 0
    while (restantes.isNotEmpty()) {
        val elegiveis = restantes.filter { indice ->
            val parada = paradas[indice]
            parada.tipo == TipoParada.COLETA || parada.pedidoIds.all { it in coletados || it !in pedidosComColeta }
        }
        val proximo = elegiveis.minByOrNull { matriz[atual][it + 1] ?: Double.POSITIVE_INFINITY }
            ?: throw IllegalArgumentException("Não foi possível ordenar as coletas.")
        val duracao = matriz[atual][proximo + 1]
        require(duracao != null && duracao.isFinite() && duracao >= 0) { "Não há rota entre os endereços selecionados." }
        ordem += proximo
        restantes -= proximo
        if (paradas[proximo].tipo == TipoParada.COLETA) coletados += paradas[proximo].pedidoIds
        atual = proximo + 1
    }
    return ordem
}
