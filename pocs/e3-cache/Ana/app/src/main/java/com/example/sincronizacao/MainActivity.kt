package com.example.sincronizacao

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cadastros.data.local.entity.Produto
import com.example.cadastros.data.local.dao.ProdutoDao
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.sincronizacao.data.local.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CadastroProdutoScreen()
                }
            }
        }
    }
}

@Composable
fun CadastroProdutoScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val produtoDao = database.produtoDao()
    val coroutineScope = rememberCoroutineScope()

    val listaProdutos by produtoDao.listarTodos().collectAsState(initial = emptyList())

    var nome by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Cadastro de Produto (PoC)", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome do Produto") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = preco,
            onValueChange = { preco = it },
            label = { Text("Preço (ex: 99.90)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Button(
            onClick = {
                val precoDouble = preco.toDoubleOrNull() ?: 0.0
                if (nome.isNotBlank()) {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        // 1. Salva no SQLite
                        val novoProduto = Produto(
                            nome = nome,
                            preco = precoDouble,
                            descricao = descricao,
                            isSynced = false
                        )
                        produtoDao.inserirProduto(novoProduto)

                        // 2. Tenta sincronizar com a nuvem
                        try {
                            sincronizarComFirestore(produtoDao)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "Sincronizado com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "Salvo localmente (offline)", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Limpar campos
                        nome = ""
                        preco = ""
                        descricao = ""
                    }
                }
            },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Text("Salvar e Sincronizar")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Dados em Cache:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(listaProdutos) { produto ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "ID: ${produto.id}", style = MaterialTheme.typography.labelSmall)
                        Text(text = "Nome: ${produto.nome}", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Preço: R$ ${produto.preco}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Desc: ${produto.descricao}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private suspend fun sincronizarComFirestore(produtoDao: ProdutoDao) {
    val firestore = com.google.firebase.Firebase.firestore
    val produtosPendentes = produtoDao.buscarNaoSincronizados()

    produtosPendentes.forEach { produto ->
        try {
            firestore.collection("produtos")
                .document(produto.id)
                .set(produto)
                .await()

            produtoDao.marcarComoSincronizado(produto.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}