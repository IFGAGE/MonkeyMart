package com.example.marketplace

import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.marketplace.data.*
import com.example.marketplace.data.entity.EnderecoColeta
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.UsuarioEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NegociantePersistenceTest {
    @Test fun enderecoPersisteAposReabrirBancoESoAlteraOUsuarioCorreto() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "coleta-${UUID.randomUUID()}"
        fun abrir() = Room.databaseBuilder(context, AppDatabase::class.java, nome).build()
        val endereco = EnderecoColeta("74000000", "Rua Um", "10", "", "Centro", "Goiânia", "GO")
        var db = abrir()
        try {
            val ana = UsuarioEntity("ana@teste.com", "Ana", "", "", "", "negociante")
            val edu = ana.copy(email = "edu@teste.com", nome = "Edu")
            db.usuarioDao().salvarUsuario(ana.copy(enderecoColeta = endereco))
            db.usuarioDao().salvarUsuario(edu)
            db.close()
            db = abrir()
            assertEquals(endereco, db.usuarioDao().buscarUsuario(ana.email)!!.enderecoColeta)
            assertNull(db.usuarioDao().buscarUsuario(edu.email)!!.enderecoColeta)
            val editado = endereco.copy(numero = "25")
            db.usuarioDao().salvarUsuario(ana.copy(enderecoColeta = editado))
            assertEquals("25", db.usuarioDao().observarUsuario(ana.email).first()!!.enderecoColeta!!.numero)
            assertFalse(db.usuarioDao().buscarUsuario(ana.email)!!.isSynced)

            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = ana.email,
                nomeNegociante = "Ana", nome = "Livro", preco = "10", descricao = "Usado"))
            db.produtoDao().salvarProduto(ProdutoEntity(emailNegociante = edu.email,
                nomeNegociante = "Edu", nome = "Livro", preco = "15", descricao = "Novo"))
            assertEquals(listOf("Ana"), db.produtoDao().buscarProdutosDoNegociante(ana.email).first().map { it.nomeNegociante })
            assertEquals(listOf("Edu"), db.produtoDao().buscarTodosProdutos().first()
                .filter { it.podeSerCompradoPor(ana.email) }.map { it.nomeNegociante })
        } finally {
            db.close()
            context.deleteDatabase(nome)
        }
    }

    @Test fun migracaoPreservaCadastroExistenteSemInventarEndereco() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val nome = "migracao-coleta-${UUID.randomUUID()}"
        val path = context.getDatabasePath(nome)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { antigo ->
            instrumentation.context.assets.open("schema3.sql").bufferedReader().use { reader ->
                reader.readText().split(';').filter { it.isNotBlank() }.forEach { antigo.execSQL(it) }
            }
            antigo.execSQL("INSERT INTO tabela_usuario VALUES ('ana@teste.com', 'Ana', '62999999999', '12345678901', '01012000', 'negociante', 1)")
            antigo.version = 3
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nome)
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build()
        try {
            val usuario = db.usuarioDao().buscarUsuario("ana@teste.com")!!
            assertEquals("Ana", usuario.nome)
            assertEquals("12345678901", usuario.cpf)
            assertTrue(usuario.isSynced)
            assertNull(usuario.enderecoColeta)
        } finally {
            db.close()
            context.deleteDatabase(nome)
        }
    }
}
