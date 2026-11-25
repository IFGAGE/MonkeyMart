package com.example.marketplace

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.notificacoes.HistoricoNotificacoes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HistoricoNotificacoesTest {
    @Test fun guardaPedidoELeituraEimpedeQueAvisoLimpoVolteNaSincronizacao() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val conta = UUID.randomUUID().toString()
        val outraConta = UUID.randomUUID().toString()
        val id = "comprador:pedido-teste"
        val historico = HistoricoNotificacoes
        try {
            historico.registrar(context, conta, id, "Entregador por perto", "Aproximação", 13)
            historico.registrar(context, outraConta, id, "Entregador por perto", "Aproximação", 7)

            val aviso = historico.observar(context, conta).first().single()
            assertEquals(13, aviso.pedidoId)
            assertFalse(aviso.lida)
            historico.marcarComoLida(context, conta, id)
            assertTrue(historico.observar(context, conta).first().single().lida)

            historico.limpar(context, conta)
            historico.registrar(context, conta, id, "Entregador por perto", "Aproximação", 13)
            assertTrue(historico.foiLimpa(context, conta, id))
            assertTrue(historico.observar(context, conta).first().isEmpty())
            assertFalse(historico.observar(context, outraConta).first().single().lida)

            historico.registrar(context, conta, "comprador:novo", "Entregador por perto", "Aproximação", 14)
            assertEquals(14, historico.observar(context, conta).first().single().pedidoId)
        } finally {
            for (usuario in listOf(conta, outraConta)) {
                context.getSharedPreferences("historico_notificacoes_$usuario", 0).edit().clear().commit()
            }
        }
    }
}
