package com.example.sincronizacao

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TelaProdutos()
                }
            }
        }
    }
}

@Composable
fun TelaProdutos() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).produtoDao() }
    val scope = rememberCoroutineScope()

    val produtos by dao.listarTodos().collectAsState(initial = emptyList())
    var nome by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("PoC: cache local (SQLite) + Firestore", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(preco, { preco = it }, label = { Text("Preço") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (nome.isBlank()) return@Button
                val produto = Produto(nome = nome, preco = preco.toDoubleOrNull() ?: 0.0)
                nome = ""
                preco = ""

                scope.launch(Dispatchers.IO) {
                    dao.inserir(produto)


                    val resultado = try {
                        sincronizar(dao)
                        "Sincronizado!"
                    } catch (e: Exception) {
                        "Salvo só localmente (sem internet)"
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resultado, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Cache local:", style = MaterialTheme.typography.titleSmall)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(produtos) { produto ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("${produto.nome} - R$ ${produto.preco}")
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(if (produto.isSynced) "  ☁ sincronizado" else "  ⏳ pendente")
                }
            }
        }
    }
}


private suspend fun sincronizar(dao: ProdutoDao) {
    val firestore = Firebase.firestore
    dao.listarPendentes().forEach { produto ->
        firestore.collection("produtos").document(produto.id).set(produto).await()
        dao.marcarComoSincronizado(produto.id)
    }
}
