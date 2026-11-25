package com.example.marketplace

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.acompanhamento.processarPosicaoEntrega
import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.proximidade.pontoAoNorte
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AcompanhamentoEntregaTest {
    private val entregador = "entregador@teste.invalid"
    private val destino = Coordenadas(-21.784, -48.2107)

    @Test fun aproximaEAvisaSemConfirmarColetaOuEntregaEContinuaAtualizando() = runBlocking {
        comBanco { db, pedido ->
            val agora = System.currentTimeMillis()
            val distancias = mutableListOf<Double>()
            val avisos = mutableListOf<List<Int>>()
            for (metros in listOf(400.0, 250.0, 150.0, 100.0)) {
                val ponto = pontoAoNorte(destino, metros)
                val (posicoes, novosAvisos) = processarPosicaoEntrega(db, entregador, ponto.latitude, ponto.longitude, 10f, agora, agora)
                distancias += posicoes.getValue(pedido.firestoreId).distanciaMetros()
                avisos += novosAvisos
            }
            assertTrue(distancias.zipWithNext().all { (antes, depois) -> depois < antes })
            assertEquals(listOf(emptyList(), emptyList(), listOf(pedido.id), emptyList()), avisos)
            val salvo = db.pedidoDao().buscarPorFirestoreId(pedido.firestoreId)!!
            assertEquals("EM_ROTA", salvo.statusEntrega)
            assertFalse(salvo.coletado)
            assertTrue(salvo.avisoProximidade)
            assertFalse(salvo.isSynced)
        }
    }

    @Test fun marcarEntregueForaDaAreaNaoCriaAvisoEEncerraAsPosicoes() = runBlocking {
        comBanco { db, pedido ->
            val dao = db.pedidoDao()
            val agora = System.currentTimeMillis()
            dao.gravarColetas(listOf(pedido.id), entregador, true)
            dao.atualizarEntregas(listOf(pedido.id), entregador, true)
            val ponto = pontoAoNorte(destino, 100.0)
            val (posicoes, avisos) = processarPosicaoEntrega(db, entregador, ponto.latitude, ponto.longitude, 10f, agora, agora)
            assertTrue(posicoes.isEmpty())
            assertTrue(avisos.isEmpty())
            val salvo = dao.buscarPorFirestoreId(pedido.firestoreId)!!
            assertEquals("ENTREGUE", salvo.statusEntrega)
            assertFalse(salvo.avisoProximidade)
        }
    }

    @Test fun ignoraPosicaoAntigaEContaErrada() = runBlocking {
        comBanco { db, pedido ->
            val agora = System.currentTimeMillis()
            val ponto = pontoAoNorte(destino, 100.0)
            assertTrue(processarPosicaoEntrega(db, entregador, ponto.latitude, ponto.longitude, 10f, agora - 40_000, agora).first.isEmpty())
            assertTrue(processarPosicaoEntrega(db, "outro@teste.invalid", ponto.latitude, ponto.longitude, 10f, agora, agora).first.isEmpty())
            assertFalse(db.pedidoDao().buscarPorFirestoreId(pedido.firestoreId)!!.avisoProximidade)
        }
    }

    private suspend fun comBanco(teste: suspend (AppDatabase, PedidoEntity) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "acompanhamento-${UUID.randomUUID()}"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        try {
            val pedido = PedidoEntity(emailCliente = "cliente@teste.invalid", enderecoEntrega = "Rua A", resumoItens = "Produto", valorTotal = "10",
                statusEntrega = "EM_ROTA", emailEntregador = entregador,
                entregaLatitude = destino.latitude, entregaLongitude = destino.longitude)
            db.pedidoDao().salvarPedido(pedido)
            val salvo = db.pedidoDao().buscarPorFirestoreId(pedido.firestoreId)!!
            db.rotaDao().salvar(RotaEntity(emailEntregador = entregador, estado = "INICIADA", pedidosJson = "[${salvo.id}]",
                origemJson = "{}", destinosJson = "[]", geometriaJson = "[]"))
            teste(db, salvo)
        } finally { db.close(); context.deleteDatabase(nome) }
    }
}
