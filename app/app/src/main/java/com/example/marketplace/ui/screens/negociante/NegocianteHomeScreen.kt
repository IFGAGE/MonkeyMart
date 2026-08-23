package com.example.marketplace.ui.screens.negociante

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items


// Modelo atualizado com Nome do Vendedor e Foto!
data class ProdutoTemp(
    val nome: String,
    val preco: String,
    val descricao: String,
    val nomeDono: String,
    val fotoPathLocal: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegocianteHomeScreen(
    produtos: List<ProdutoTemp>,
    onAddProdutoClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val logo = if (isDarkTheme) R.drawable.mm_branco else R.drawable.mm_preto

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = logo),
                        contentDescription = "Logo Monkey Mart",
                        modifier = Modifier.height(40.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAddProdutoClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar Produto")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ListaProdutos(produtos)
        }
    }
}

@Composable
fun ListaProdutos(produtos: List<ProdutoTemp>) {
    // Trocamos o LazyColumn pelo LazyVerticalGrid
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 colunas!
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(produtos) { produto ->
            ProdutoItemCard(produto)
        }
    }
}

// O Card do Produto individual (Agora no formato de Grid/Vitrine)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutoItemCard(produto: ProdutoTemp) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 1. A IMAGEM EM CIMA (Provisória por enquanto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp) // Altura fixa para a foto ficar padrão na grade
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ImageIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Gray
                )
            }

            // 2. OS DADOS EMBAIXO
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = produto.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1, // Impede que o título quebre o layout se for muito longo
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
            }
        }
    }

    // O Modal que desliza de baixo pra cima (Continua igualzinho!)
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
                    Text(text = produto.nome, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(text = "R$ ${produto.preco}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Vendido por ${produto.nomeDono}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Descrição", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = produto.descricao, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)

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