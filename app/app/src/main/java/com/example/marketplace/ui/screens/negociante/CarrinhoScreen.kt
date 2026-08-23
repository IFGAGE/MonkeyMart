package com.example.marketplace.ui.screens.negociante

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrinhoScreen(
    itensCarrinho: List<ItemCarrinho>,
    onAumentar: (ItemCarrinho) -> Unit,
    onDiminuir: (ItemCarrinho) -> Unit,
    onRemover: (ItemCarrinho) -> Unit,
    onFinalizarPedido: (endereco: String, total: String) -> Unit,
    onVoltar: () -> Unit
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var endereco by remember { mutableStateOf("") }
    var erroEndereco by remember { mutableStateOf(false) }
    var mostrarSucesso by remember { mutableStateOf(false) }

    val totalDouble = itensCarrinho.sumOf { item ->
        val precoNumerico = item.produto.preco.replace(",", ".").toDoubleOrNull() ?: 0.0
        precoNumerico * item.quantidade
    }
    val totalFormatado = String.format("%.2f", totalDouble)

    if (mostrarSucesso) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sucesso",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pedido Concluído!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("O entregador já foi notificado.", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        onFinalizarPedido(endereco, totalFormatado)
                    },
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Voltar para o Início", fontSize = 16.sp)
                }
            }
        }
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
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Ir para o Pagamento", fontSize = 16.sp)
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

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Finalizar Pedido") },
            text = {
                Column {
                    Text("Você está prestes a pagar R$ $totalFormatado.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = endereco,
                        onValueChange = {
                            endereco = it
                            erroEndereco = false
                        },
                        label = { Text("Endereço de Entrega") },
                        isError = erroEndereco,
                        supportingText = {
                            if (erroEndereco) {
                                Text("Obrigatório informar o endereço.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (endereco.isNotBlank()) {
                            showCheckoutDialog = false
                            mostrarSucesso = true
                        } else {
                            erroEndereco = true
                        }
                    }
                ) {
                    Text("Pagar e Finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) { Text("Cancelar") }
            }
        )
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
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ImageIcon,
                    contentDescription = null,
                    tint = Color.Gray
                )
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
                        .background(Color.LightGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.ImageIcon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Text("Câmera / Galeria na Semana 9", modifier = Modifier.padding(top = 80.dp), color = Color.Gray, fontWeight = FontWeight.SemiBold)
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