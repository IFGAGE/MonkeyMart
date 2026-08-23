package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketplace.R
import kotlinx.coroutines.launch

data class AvaliacaoTemp(
    val nota: Int,
    val comentario: String,
    val emailAutor: String
)

data class ProdutoTemp(
    val nome: String,
    val preco: String,
    val descricao: String,
    val nomeDono: String,
    val fotoPathLocal: String?,
    val avaliacoes: List<AvaliacaoTemp> = emptyList(),
    val podeAvaliar: Boolean = false
)

data class ItemCarrinho(
    val produto: ProdutoTemp,
    var quantidade: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegocianteHomeScreen(
    produtos: List<ProdutoTemp>,
    quantidadeCarrinho: Int,
    onAddProdutoClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCartClick: () -> Unit,
    onAddToCart: (ProdutoTemp) -> Unit,
    onEnviarAvaliacao: (nomeProduto: String, nota: Int, comentario: String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val logo = if (isDarkTheme) R.drawable.mm_branco else R.drawable.mm_preto
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = logo),
                        contentDescription = "Logo",
                        modifier = Modifier.height(40.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAddProdutoClick) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCartClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50)
            ) {
                if (quantidadeCarrinho > 0) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(if (quantidadeCarrinho > 99) "99+" else quantidadeCarrinho.toString())
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Carrinho")
                    }
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Carrinho")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(produtos) { produto ->
                    ProdutoItemCard(
                        produto = produto,
                        onAddToCart = {
                            onAddToCart(it)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = "Adicionado ao carrinho!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onEnviarAvaliacao = onEnviarAvaliacao
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutoItemCard(
    produto: ProdutoTemp,
    onAddToCart: (ProdutoTemp) -> Unit,
    onEnviarAvaliacao: (String, Int, String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ImageIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Gray
                )

                if (produto.avaliacoes.isNotEmpty()) {
                    val media = produto.avaliacoes.map { it.nota }.average()
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", media),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = produto.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ ${produto.preco}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vendido por ${produto.nomeDono}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onAddToCart(produto) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar", fontSize = 12.sp)
                }
            }
        }
    }

    if (showSheet) {
        val scrollState = rememberScrollState()

        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            // MUDANÇA 1: Força o modal a não ficar travado no meio da tela
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
                    // MUDANÇA 2: Avisa o Compose para empurrar a tela pra cima quando o teclado abrir
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageIcon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )

                    if (produto.avaliacoes.isNotEmpty()) {
                        val media = produto.avaliacoes.map { it.nota }.average()
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", media),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = produto.nome, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "R$ ${produto.preco}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vendido por ${produto.nomeDono}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onAddToCart(produto)
                        showSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar ao Carrinho")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Descrição", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = produto.descricao,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Avaliações", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (produto.avaliacoes.isEmpty()) {
                    Text(
                        text = "Nenhuma avaliação ainda.",
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    produto.avaliacoes.forEach { avaliacao ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = avaliacao.emailAutor.substringBefore("@"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row {
                                        for (i in 1..5) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (i <= avaliacao.nota) Color(0xFFFFC107) else Color.LightGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                if (avaliacao.comentario.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = avaliacao.comentario, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (produto.podeAvaliar) {
                    FormularioAvaliacao(
                        nomeProduto = produto.nome,
                        onEnviar = { nota, comentario ->
                            onEnviarAvaliacao(produto.nome, nota, comentario)
                        }
                    )
                }

                // Aumentado um pouco o espaço final para garantir que o botão não encoste no teclado
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun FormularioAvaliacao(
    nomeProduto: String,
    onEnviar: (nota: Int, comentario: String) -> Unit
) {
    var notaAtual by remember { mutableStateOf(0) }
    var comentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(24.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Você comprou este produto. Deixe uma avaliação!",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Estrela $i",
                        tint = if (i <= notaAtual) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { notaAtual = i }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = { Text("Comentário (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (notaAtual > 0) {
                        enviando = true
                        onEnviar(notaAtual, comentario)
                    }
                },
                enabled = notaAtual > 0 && !enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Avaliação")
            }
        }
    }
}