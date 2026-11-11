package com.example.marketplace.data.notificacoes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import org.json.JSONException
import org.json.JSONObject

internal const val TITULO_PEDIDO_ENTREGUE = "Pedido entregue"
internal const val MENSAGEM_PEDIDO_ENTREGUE = "Seu pedido já foi entregue."
private const val MENSAGEM_ANTIGA = "O entregador se aproximou antes de concluir a entrega."

internal data class Notificacao(
    val id: String,
    val titulo: String,
    val mensagem: String,
    val recebidaEm: Long,
    val lida: Boolean,
    val pedidoId: Int?
)

internal object HistoricoNotificacoes {
    private fun preferencias(context: Context, usuarioId: String) =
        context.getSharedPreferences("historico_notificacoes_$usuarioId", Context.MODE_PRIVATE)

    // O mesmo aviso pode voltar do Room após sincronizar ou reabrir o app.
    @Synchronized
    fun registrar(context: Context, usuarioId: String, id: String, titulo: String, mensagem: String, pedidoId: Int) {
        val preferencias = preferencias(context, usuarioId)
        if (preferencias.contains(id)) return
        val registro = JSONObject()
            .put("titulo", titulo)
            .put("mensagem", mensagem)
            .put("pedidoId", pedidoId)
            .put("recebidaEm", System.currentTimeMillis())
            .put("lida", false)
        preferencias.edit().putString(id, registro.toString()).apply()
    }

    fun observar(context: Context, usuarioId: String) = callbackFlow<List<Notificacao>> {
        val preferencias = preferencias(context, usuarioId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(ler(preferencias))
        }
        preferencias.registerOnSharedPreferenceChangeListener(listener)
        trySend(ler(preferencias))
        awaitClose { preferencias.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    @Synchronized
    fun marcarComoLida(context: Context, usuarioId: String, id: String) {
        val preferencias = preferencias(context, usuarioId)
        val registro = lerRegistro(preferencias.getString(id, null)) ?: return
        if (registro.optBoolean("lida")) return
        preferencias.edit().putString(id, registro.put("lida", true).toString()).apply()
    }

    fun foiLimpa(context: Context, usuarioId: String, id: String): Boolean =
        preferencias(context, usuarioId).getString(id, null) == ""

    @Synchronized
    fun limpar(context: Context, usuarioId: String) {
        val preferencias = preferencias(context, usuarioId)
        val limpar = preferencias.edit()
        // Guarda só as chaves já recebidas, para um aviso apagado não voltar na sincronização.
        preferencias.all.keys.forEach { limpar.putString(it, "") }
        limpar.apply()
    }

    private fun ler(preferencias: SharedPreferences): List<Notificacao> = preferencias.all.mapNotNull { (id, valor) ->
        val registro = lerRegistro(valor as? String) ?: return@mapNotNull null
        val mensagem = registro.optString("mensagem")
        val avisoAntigoDeEntrega = id.startsWith("comprador:") && mensagem == MENSAGEM_ANTIGA
        Notificacao(id, if (avisoAntigoDeEntrega) TITULO_PEDIDO_ENTREGUE else registro.optString("titulo"),
            if (avisoAntigoDeEntrega) MENSAGEM_PEDIDO_ENTREGUE else mensagem,
            registro.optLong("recebidaEm"), registro.optBoolean("lida"),
            registro.optInt("pedidoId").takeIf { it > 0 })
    }.sortedWith(compareByDescending<Notificacao> { it.recebidaEm }.thenBy { it.id })

    private fun lerRegistro(valor: String?): JSONObject? {
        if (valor.isNullOrEmpty()) return null
        return try { JSONObject(valor) } catch (_: JSONException) { null }
    }
}
