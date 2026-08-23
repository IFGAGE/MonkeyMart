package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketplace.R
import com.example.marketplace.ui.screens.negociante.ListaProdutos
import com.example.marketplace.ui.screens.negociante.ProdutoTemp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregadorHomeScreen(
    produtos: List<ProdutoTemp>,
    onAddVeiculoClick: () -> Unit,
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
                    IconButton(onClick = onAddVeiculoClick) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = "Adicionar Veículo")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) { // Clica e vai pra tela de perfil
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
            Text(
                text = "Produtos no Marketplace",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            ListaProdutos(produtos)
        }
    }
}