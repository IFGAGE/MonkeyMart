package com.example.marketplace

import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.MIGRATION_3_4
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.sync.acompanharPedidos
import com.example.marketplace.data.sync.sincronizarPedidos
import com.example.marketplace.ui.mapa.localizarEndereco
import com.example.marketplace.ui.screens.entregador.MapaEntregaScreen
import com.example.marketplace.ui.theme.MarketplaceTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

@RunWith(AndroidJUnit4::class)
class Entrega5IntegrationTest {
    @Test fun mapaExibeCoordenadasMarcadorEAtribuicao() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    MarketplaceTheme {
                        MapaEntregaScreen(1, "Praça da Sé, São Paulo, SP, Brasil", {})
                    }
                }
            }
            var mapa: MapView? = null
            esperar {
                scenario.onActivity { mapa = encontrarMapa(it.window.decorView) }
                mapa != null
            }
            scenario.onActivity {
                val marker = mapa!!.overlays.filterIsInstance<Marker>().single()
                assertTrue(marker.position.latitude in -23.57..-23.53)
                assertTrue(mapa!!.overlays.any { it is CopyrightOverlay })
                assertEquals("MonkeyMart/1.0", Configuration.getInstance().userAgentValue)
            }
            delay(3000)
            val imagem = instrumentation.uiAutomation.takeScreenshot()
            File(instrumentation.targetContext.filesDir, "mapa-entrega5.png").outputStream().use {
                imagem.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            imagem.recycle()
        }
    }

    private fun encontrarMapa(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                encontrarMapa(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    @Test fun migracaoPreservaPedidoDaVersaoAnterior() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migracao-entrega5-${UUID.randomUUID()}"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            instrumentation.context.assets.open("schema3.sql").bufferedReader().use { reader ->
                reader.readText().split(';').filter { it.isNotBlank() }.forEach { legacy.execSQL(it) }
            }
            legacy.execSQL("INSERT INTO tabela_pedido VALUES (7, 'cliente@teste.invalid', 'Rua antiga, 20', 'Produto', '10.00', 'PENDENTE', 0)")
            legacy.version = 3
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_3_4).build()
        try {
            val pedido = db.pedidoDao().buscarPorFirestoreId("7")!!
            assertEquals(7, pedido.id)
            assertEquals("Rua antiga, 20", pedido.enderecoEntrega)
            assertFalse(pedido.isSynced)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun pedidosEntreDoisCachesEReenvioOffline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "entrega5-${UUID.randomUUID()}"
        val a = Room.databaseBuilder(context, AppDatabase::class.java, "$prefix-a").addMigrations(MIGRATION_3_4).build()
        val b = Room.databaseBuilder(context, AppDatabase::class.java, "$prefix-b").addMigrations(MIGRATION_3_4).build()
        val firestore = FirebaseFirestore.getInstance()
        FirebaseAuth.getInstance().signInAnonymously().await()
        val cliente = PedidoEntity(emailCliente="cliente@teste.invalid", enderecoEntrega="Praça da Sé, São Paulo, SP",
            resumoItens="Produto A", valorTotal="10.00")
        val outro = PedidoEntity(emailCliente="outro@teste.invalid", enderecoEntrega="Rua B, 20",
            resumoItens="Produto B", valorTotal="20.00")
        var receiverA: Job? = null
        var receiverB: Job? = null
        try {
            // Ambos começam com id local 1. Os documentos na nuvem precisam ser diferentes.
            a.pedidoDao().salvarPedido(cliente)
            b.pedidoDao().salvarPedido(outro)
            sincronizarPedidos(a.pedidoDao())
            sincronizarPedidos(b.pedidoDao())
            assertNotEquals(cliente.firestoreId, outro.firestoreId)
            assertTrue(firestore.collection("pedidos").document(cliente.firestoreId).get().await().exists())
            receiverA = launch { acompanharPedidos(a.pedidoDao()) }
            receiverB = launch { acompanharPedidos(b.pedidoDao()) }
            esperar { b.pedidoDao().buscarPorFirestoreId(cliente.firestoreId) != null }
            val recebido = b.pedidoDao().buscarPorFirestoreId(cliente.firestoreId)!!
            assertEquals(cliente.enderecoEntrega, recebido.enderecoEntrega)
            assertNotEquals(1, recebido.id)
            b.pedidoDao().atualizarStatus(recebido.id, "ENTREGUE")
            sincronizarPedidos(b.pedidoDao())
            esperar { a.pedidoDao().buscarPorFirestoreId(cliente.firestoreId)?.statusEntrega == "ENTREGUE" }
            assertEquals(recebido.id, b.pedidoDao().buscarPorFirestoreId(cliente.firestoreId)!!.id)

            firestore.disableNetwork().await()
            val offline = cliente.copy(id=0, firestoreId=UUID.randomUUID().toString(), isSynced=false)
            a.pedidoDao().salvarPedido(offline)
            assertFalse(a.pedidoDao().buscarPorFirestoreId(offline.firestoreId)!!.isSynced)
            val upload = launch { sincronizarPedidos(a.pedidoDao()) }
            delay(300)
            assertFalse(a.pedidoDao().buscarPorFirestoreId(offline.firestoreId)!!.isSynced)
            firestore.enableNetwork().await()
            withTimeout(30_000) { upload.join() }
            esperar { b.pedidoDao().buscarPorFirestoreId(offline.firestoreId) != null }
            assertTrue(a.pedidoDao().buscarPorFirestoreId(offline.firestoreId)!!.isSynced)
        } finally {
            receiverA?.cancelAndJoin()
            receiverB?.cancelAndJoin()
            firestore.enableNetwork().await()
            a.close()
            b.close()
            context.deleteDatabase("$prefix-a")
            context.deleteDatabase("$prefix-b")
        }
    }

    @Test fun geocoderEncontraCoordenadasDeEnderecoReal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ponto = withTimeout(30_000) { localizarEndereco(context, "Praça da Sé, São Paulo, SP, Brasil") }
        assertNotNull(ponto)
        assertTrue(ponto!!.latitude in -23.57..-23.53)
        assertTrue(ponto.longitude in -46.66..-46.61)
    }

    private suspend fun esperar(condicao: suspend () -> Boolean) {
        withTimeout(30_000) { while (!condicao()) delay(100) }
    }
}
