package com.example.marketplace

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PedidoVendedorTest {
    @Test fun compraPreparoVeiculoERetiradaSaoSeparadosPorVendedor() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "pedidos-vendedor-${UUID.randomUUID()}"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        try {
            val coleta = EnderecoColeta(endereco = "Rua de coleta, 10", latitude = -16.7, longitude = -49.3)
            val vendedor = UsuarioEntity("ana", "Ana", "", "", "", "negociante", enderecoColeta = coleta)
            db.usuarioDao().salvarUsuario(vendedor)
            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = "ana", nomeNegociante = "Ana",
                nome = "Livro", preco = "10", descricao = "", enderecoColeta = coleta.endereco,
                coletaLatitude = coleta.latitude, coletaLongitude = coleta.longitude))
            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = "edu", nomeNegociante = "Edu",
                nome = "Livro", preco = "20", descricao = "", enderecoColeta = "Coleta do Edu",
                coletaLatitude = -16.8, coletaLongitude = -49.4))
            val produtos = db.produtoDao().buscarTodosProdutos().first()
            db.pedidoDao().finalizarCompra("cliente", produtos.associate { it.id to 2 },
                EnderecoValidado("Entrega", -16.9, -49.5))
            assertEquals(2, db.pedidoDao().buscarPedidosDoCliente("cliente").first().size)
            val pedido = db.pedidoDao().buscarPedidosDoVendedor("ana").first().single()
            assertEquals("20.00", pedido.valorTotal)
            assertTrue(db.pedidoDao().buscarPedidosPendentes().first().isEmpty())
            assertEquals(0, db.pedidoDao().marcarPronto(pedido.id, "edu"))
            assertEquals(1, db.pedidoDao().marcarPronto(pedido.id, "ana"))
            assertEquals(listOf(pedido.id), db.pedidoDao().buscarPedidosPendentes().first().map { it.id })

            db.usuarioDao().salvarColeta(vendedor.copy(enderecoColeta = coleta.copy(endereco = "Coleta nova")))
            assertEquals("Rua de coleta, 10", db.pedidoDao().buscarPorFirestoreId(pedido.firestoreId)!!.enderecoColeta)
            assertEquals("Coleta nova", db.produtoDao().buscarProdutosDoNegociante("ana").first().single().enderecoColeta)

            db.rotaDao().salvar(RotaEntity("motorista", pedidosJson = "[${pedido.id}]",
                origemJson = "{}", destinosJson = "[]", geometriaJson = "[]"))
            try {
                db.rotaDao().iniciar("motorista", "ABC1D23", listOf(pedido.id))
                fail("Não pode iniciar sem veículo")
            } catch (_: IllegalStateException) { }
            assertEquals("PLANEJADA", db.rotaDao().observar("motorista").first()!!.estado)
            db.veiculoDao().salvarVeiculo(VeiculoEntity("ABC1D23", "motorista", "Modelo", "Marca", "2020"))
            db.rotaDao().selecionarVeiculo(db.rotaDao().observar("motorista").first()!!, "ABC1D23")
            assertEquals("ABC1D23", db.rotaDao().observar("motorista").first()!!.placaVeiculo)
            assertEquals(1, db.rotaDao().marcarReservando("motorista", "ABC1D23"))
            try {
                db.rotaDao().excluirPlanejada("motorista")
                fail("Não pode apagar reserva pendente sem conciliação remota")
            } catch (_: IllegalStateException) { }
            db.rotaDao().iniciar("motorista", "ABC1D23", listOf(pedido.id))
            assertTrue(db.pedidoDao().buscarPedidosPendentes().first().isEmpty())
            assertEquals(0, db.pedidoDao().atualizarEntrega(pedido.id, "outro motorista", "ENTREGUE"))
            assertEquals(0, db.pedidoDao().atualizarEntrega(pedido.id, "motorista", "ENTREGUE"))
            try {
                db.rotaDao().finalizar("motorista", listOf(pedido.id))
                fail("Não pode encerrar antes das coletas e entregas")
            } catch (_: IllegalStateException) { }
            db.pedidoDao().atualizarColetas(listOf(pedido.id), "motorista", true)
            try {
                db.rotaDao().cancelar("motorista", listOf(pedido.id))
                fail("Não pode liberar a outro motorista um pedido já coletado")
            } catch (_: IllegalStateException) { }
            assertEquals(1, db.pedidoDao().atualizarEntrega(pedido.id, "motorista", "ENTREGUE"))
            try {
                db.pedidoDao().atualizarColetas(listOf(pedido.id), "motorista", false)
                fail("Não pode desfazer uma coleta já entregue")
            } catch (_: IllegalStateException) { }
            db.rotaDao().finalizar("motorista", listOf(pedido.id))
            assertNull(db.rotaDao().observar("motorista").first())
        } finally { db.close(); context.deleteDatabase(nome) }
    }

    @Test fun coletaInvalidaEmUmVendedorImpedeTodaACompra() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "compra-atomica-${UUID.randomUUID()}"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        try {
            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = "ana", nomeNegociante = "Ana",
                nome = "Livro", preco = "10", descricao = "", enderecoColeta = "Rua A", coletaLatitude = -16.0, coletaLongitude = -49.0))
            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = "edu", nomeNegociante = "Edu",
                nome = "Livro", preco = "20", descricao = ""))
            val produtos = db.produtoDao().buscarTodosProdutos().first()
            try {
                db.pedidoDao().finalizarCompra("cliente", produtos.associate { it.id to 1 }, EnderecoValidado("Entrega", -16.0, -49.0))
                fail("Compra inválida deveria falhar")
            } catch (_: IllegalArgumentException) { }
            assertTrue(db.pedidoDao().buscarPedidosDoCliente("cliente").first().isEmpty())
        } finally { db.close(); context.deleteDatabase(nome) }
    }
}
