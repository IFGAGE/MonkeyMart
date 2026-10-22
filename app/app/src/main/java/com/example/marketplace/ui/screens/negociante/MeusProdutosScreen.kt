package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.marketplace.data.entity.ProdutoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusProdutosScreen(produtos: List<ProdutoEntity>, onVoltar: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Meus produtos") }, navigationIcon = {
            IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, "Voltar") }
        })
    }) { padding ->
        if (produtos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Você ainda não cadastrou produtos.")
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(produtos, key = { it.id }) { produto ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (produto.fotoPathLocal != null) {
                                AsyncImage(produto.fotoPathLocal, "Foto de ${produto.nome}",
                                    modifier = Modifier.size(80.dp), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Inventory2, null, Modifier.size(80.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(produto.nome, style = MaterialTheme.typography.titleMedium)
                                Text("R$ ${produto.preco}", color = MaterialTheme.colorScheme.primary)
                                Text(produto.descricao, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
