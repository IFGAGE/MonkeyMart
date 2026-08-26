package com.example.marketplace.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuScreen(
    tipoPerfil: String,
    onMeusDadosClick: () -> Unit,
    onMeusPedidosClick: () -> Unit,
    onMeusVeiculosClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Principal") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Voltar") }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Meus Dados", fontSize = 18.sp) },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onMeusDadosClick() }
            )
            HorizontalDivider()

            if (tipoPerfil == "negociante") {
                ListItem(
                    headlineContent = { Text("Meus Pedidos", fontSize = 18.sp) },
                    leadingContent = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onMeusPedidosClick() }
                )
            } else {
                ListItem(
                    headlineContent = { Text("Meus Veículos", fontSize = 18.sp) },
                    leadingContent = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onMeusVeiculosClick() }
                )
            }
            HorizontalDivider()

            Spacer(modifier = Modifier.weight(1f))

            ListItem(
                headlineContent = { Text("Sair da conta", fontSize = 18.sp, color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onSignOutClick() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}