package com.example.marketplace.ui.screens.negociante

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.marketplace.data.entity.EnderecoValidado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrinhoScreen(
    itensCarrinho: List<ItemCarrinho>,
    onAumentar: (ItemCarrinho) -> Unit,
    onDiminuir: (ItemCarrinho) -> Unit,
    onRemover: (ItemCarrinho) -> Unit,
    onFinalizarPedido: suspend (endereco: EnderecoValidado, total: String) -> Unit,
    onVoltar: () -> Unit
) {
    var finalizandoCompra by rememberSaveable { mutableStateOf(false) }
    var endereco by remember { mutableStateOf<EnderecoValidado?>(null) }
    var mostrarSucesso by rememberSaveable { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var erroSalvar by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val totalDouble = itensCarrinho.sumOf { item ->
        val precoNumerico = item.produto.preco.replace(",", ".").toDoubleOrNull() ?: 0.0
        precoNumerico * item.quantidade
    }
    val totalFormatado = String.format("%.2f", totalDouble)

    if (mostrarSucesso) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sucesso",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(88.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Compra concluída!", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Acompanhe seus pedidos em Minhas compras.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        onVoltar()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Voltar ao início", fontSize = 16.sp)
                }
            }
        }
        return
    }

    if (finalizandoCompra) {
        BackHandler { if (!salvando) finalizandoCompra = false }
        FinalizarCompraScreen(total = totalFormatado,
            quantidadeVendedores = itensCarrinho.map { it.produto.emailDono }.distinct().size,
            salvando = salvando, podeFinalizar = endereco?.valido() == true, erro = erroSalvar,
            onEndereco = { endereco = it; erroSalvar = null },
            onVoltar = { if (!salvando) finalizandoCompra = false },
            onConfirmar = {
                val confirmado = endereco
                if (confirmado?.valido() == true && !salvando) {
                    salvando = true
                    erroSalvar = null
                    scope.launch {
                        try {
                            onFinalizarPedido(confirmado, totalFormatado)
                            finalizandoCompra = false
                            mostrarSucesso = true
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { erroSalvar = e.message ?: "Não foi possível concluir a compra. Tente novamente." }
                        finally { salvando = false }
                    }
                }
            })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Carrinho") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, "Voltar") }
                }
            )
        },
        bottomBar = {
            if (itensCarrinho.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("R$ $totalFormatado", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { endereco = null; erroSalvar = null; finalizandoCompra = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Continuar compra", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (itensCarrinho.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Seu carrinho está vazio.", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(itensCarrinho) { item ->
                        ItemCarrinhoCard(item, onAumentar, onDiminuir, onRemover)
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCarrinhoCard(
    item: ItemCarrinho,
    onAumentar: (ItemCarrinho) -> Unit,
    onDiminuir: (ItemCarrinho) -> Unit,
    onRemover: (ItemCarrinho) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!item.produto.fotoPathLocal.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.produto.fotoPathLocal,
                        contentDescription = "Foto de ${item.produto.nome}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ImageIcon,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.produto.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Vendido por ${item.produto.nomeDono}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ ${item.produto.preco}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (item.quantidade > 1) onDiminuir(item) else onRemover(item)
                }) {
                    Icon(if (item.quantidade > 1) Icons.Default.Remove else Icons.Default.Delete, contentDescription = "Diminuir")
                }

                Text(
                    text = item.quantidade.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(onClick = { onAumentar(item) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.produto.fotoPathLocal.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.produto.fotoPathLocal,
                            contentDescription = "Foto de ${item.produto.nome}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(imageVector = Icons.Default.ImageIcon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.produto.nome, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(text = "R$ ${item.produto.preco}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Vendido por ${item.produto.nomeDono}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Descrição", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.produto.descricao, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Comentários", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    text = "Nenhum comentário ainda. (Em breve)",
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
