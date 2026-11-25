package com.example.marketplace

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.dao.AvisoDeProximidade
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.RotaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AvisoProximidadeTest {
    @Test fun registraUmaVezSemDependerDaColetaEExibeSomenteAoComprador() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "proximidade-${UUID.randomUUID()}"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        try {
            val dao = db.pedidoDao()
            val pedido = PedidoEntity(
                emailCliente = "comprador@teste.invalid",
                emailEntregador = "entregador@teste.invalid",
                enderecoEntrega = "Rua A, 10",
                resumoItens = "Produto",
                valorTotal = "10",
                entregaLatitude = -23.5505,
                entregaLongitude = -46.6333,
                statusEntrega = "EM_ROTA",
                isSynced = true
            )
            dao.salvarPedido(pedido)
            val id = dao.buscarPorFirestoreId(pedido.firestoreId)!!.id
            val ids = listOf(id)
            val entregador = pedido.emailEntregador
            assertEquals(0, dao.registrarProximidade(ids, entregador))

            db.rotaDao().salvar(RotaEntity(
                emailEntregador = entregador,
                estado = "INICIADA",
                pedidosJson = ids.toString(),
                origemJson = "{}",
                destinosJson = "[]",
                geometriaJson = "[]"
            ))
            assertEquals(ids, dao.observarDestinosMonitoraveis(entregador).first().map { it.id })
            assertEquals(0, dao.registrarProximidade(ids, "outro@teste.invalid"))
            assertEquals(1, dao.registrarProximidade(ids, entregador))
            assertEquals(0, dao.registrarProximidade(ids, entregador))

            val salvo = dao.buscarPorFirestoreId(pedido.firestoreId)!!
            assertTrue(salvo.avisoProximidade)
            assertFalse(salvo.isSynced)
            assertFalse(salvo.coletado)
            assertEquals(1, salvo.revisao)
            assertTrue(dao.observarDestinosMonitoraveis(entregador).first().isEmpty())
            assertEquals(listOf(AvisoDeProximidade(pedido.firestoreId, "EM_ROTA", id)),
                dao.observarAvisosDeProximidade(pedido.emailCliente).first())
            assertTrue(dao.observarAvisosDeProximidade("outro@teste.invalid").first().isEmpty())
            dao.gravarColetas(ids, entregador, true)
            assertEquals(1, dao.atualizarEntrega(id, entregador, "ENTREGUE"))
            assertEquals(listOf(AvisoDeProximidade(pedido.firestoreId, "ENTREGUE", id)),
                dao.observarAvisosDeProximidade(pedido.emailCliente).first())
        } finally {
            db.close()
            context.deleteDatabase(nome)
        }
    }
    @Test fun eventoDeGeofenceConfereRotaAtualCoordenadasERepeticao() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "geofence-${UUID.randomUUID()}"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        try {
            val dao = db.pedidoDao()
            val entregador = "entregador@teste.invalid"
            val base = PedidoEntity(emailCliente = "cliente@teste.invalid", emailEntregador = entregador,
                enderecoEntrega = "Rua A", resumoItens = "Produto", valorTotal = "10", coletado = true,
                statusEntrega = "EM_ROTA", entregaLatitude = -21.784, entregaLongitude = -48.2107)
            dao.salvarPedido(base)
            val id = dao.buscarPorFirestoreId(base.firestoreId)!!.id
            val outro = base.copy(firestoreId = UUID.randomUUID().toString())
            dao.salvarPedido(outro)
            val outroId = dao.buscarPorFirestoreId(outro.firestoreId)!!.id
            val ponto = "-21.784,-48.2107"
            assertEquals(emptyList<Int>(), dao.registrarEntradaGeofence(entregador, mapOf(id to ponto)))
            db.rotaDao().salvar(RotaEntity(emailEntregador = entregador, estado = "INICIADA",
                pedidosJson = "[$id]", origemJson = "{}", destinosJson = "[]", geometriaJson = "[]"))
            assertEquals(emptyList<Int>(), dao.registrarEntradaGeofence(entregador, mapOf(outroId to ponto)))
            assertEquals(emptyList<Int>(), dao.registrarEntradaGeofence(entregador, mapOf(id to "-23.55,-46.63")))
            assertEquals(emptyList<Int>(), dao.registrarEntradaGeofence("outro@teste.invalid", mapOf(id to ponto)))
            assertEquals(listOf(id), dao.registrarEntradaGeofence(entregador, mapOf(id to ponto)))
            assertEquals(emptyList<Int>(), dao.registrarEntradaGeofence(entregador, mapOf(id to ponto)))
            assertFalse(dao.buscarPorFirestoreId(outro.firestoreId)!!.avisoProximidade)
        } finally {
            db.close()
            context.deleteDatabase(nome)
        }
    }

}
