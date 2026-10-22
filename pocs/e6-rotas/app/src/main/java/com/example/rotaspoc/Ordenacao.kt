package com.example.rotaspoc

// A linha/coluna zero é a localização atual. Os índices retornados se referem
// às entradas: índice 0 = linha 1 da matriz. A rota termina na última parada.
fun ordenarParadas(matriz: List<List<Double?>>): List<Int> {
    val n = matriz.size - 1
    require(n >= 2 && matriz.all { it.size == n + 1 }) { "Informe pelo menos dois endereços válidos." }
    require(matriz.flatten().all { it == null || it.isFinite() && it >= 0 }) { "Tempos de viagem inválidos." }
    if (n > 12) return ordenarPorVizinho(matriz)

    // Held–Karp: menor custo para visitar o conjunto mask e terminar em ultimo.
    val estados = 1 shl n
    val custos = DoubleArray(estados * n) { Double.POSITIVE_INFINITY }
    val anterior = IntArray(custos.size) { -1 }
    for (i in 0 until n) custos[(1 shl i) * n + i] = matriz[0][i + 1] ?: Double.POSITIVE_INFINITY
    for (mask in 1 until estados) for (ultimo in 0 until n) {
        val custo = custos[mask * n + ultimo]
        if (!custo.isFinite() || mask and (1 shl ultimo) == 0) continue
        for (proximo in 0 until n) {
            if (mask and (1 shl proximo) != 0) continue
            val trecho = matriz[ultimo + 1][proximo + 1] ?: continue
            val indice = (mask or (1 shl proximo)) * n + proximo
            if (custo + trecho < custos[indice]) {
                custos[indice] = custo + trecho
                anterior[indice] = ultimo
            }
        }
    }
    var mask = estados - 1
    var ultimo = (0 until n).minBy { custos[mask * n + it] }
    require(custos[mask * n + ultimo].isFinite()) { "Não existe um caminho pelas ruas que conecte todas as paradas." }
    val reverso = mutableListOf<Int>()
    while (ultimo >= 0) {
        reverso.add(ultimo)
        val anteriorIndice = anterior[mask * n + ultimo]
        mask = mask xor (1 shl ultimo)
        ultimo = anteriorIndice
    }
    return reverso.reversed()
}

private fun ordenarPorVizinho(matriz: List<List<Double?>>): List<Int> {
    val restantes = (0 until matriz.size - 1).toMutableSet()
    val ordem = mutableListOf<Int>()
    var atual = 0
    while (restantes.isNotEmpty()) {
        val proximo = restantes.filter { matriz[atual][it + 1] != null }
            .minByOrNull { matriz[atual][it + 1]!! }
        require(proximo != null) { "Não foi possível conectar todas as paradas. Tente menos endereços." }
        ordem.add(proximo)
        restantes.remove(proximo)
        atual = proximo + 1
    }
    return ordem
}
