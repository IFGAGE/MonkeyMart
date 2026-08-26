package com.example.marketplace.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userEmail: String,
    usuario: UsuarioTemp?,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Dados") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Informações da Conta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (usuario != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(label = "Nome:", value = usuario.nome)
                        InfoRow(label = "E-mail:", value = userEmail)
                        InfoRow(label = "CPF:", value = formatarCPF(usuario.cpf))
                        InfoRow(label = "Telefone:", value = formatarTelefone(usuario.telefone))
                        InfoRow(label = "Nascimento:", value = formatarDataNascimento(usuario.dataNascimento))
                        InfoRow(label = "Perfil:", value = usuario.tipoPerfil.replaceFirstChar { it.uppercase() })
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(label = "E-mail de acesso:", value = userEmail)
                        Text(
                            text = "Dados adicionais não carregados.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}


fun formatarCPF(cpf: String): String {
    val numeros = cpf.filter { it.isDigit() }
    return if (numeros.length == 11) {
        "${numeros.substring(0, 3)}.${numeros.substring(3, 6)}.${numeros.substring(6, 9)}-${numeros.substring(9, 11)}"
    } else {
        cpf
    }
}

fun formatarTelefone(telefone: String): String {
    val numeros = telefone.filter { it.isDigit() }
    return when (numeros.length) {
        11 -> "(${numeros.substring(0, 2)}) ${numeros.substring(2, 7)}-${numeros.substring(7, 11)}"
        10 -> "(${numeros.substring(0, 2)}) ${numeros.substring(2, 6)}-${numeros.substring(6, 10)}"
        else -> telefone
    }
}

fun formatarDataNascimento(data: String): String {
    val numeros = data.filter { it.isDigit() }
    return if (numeros.length == 8) {
        "${numeros.substring(0, 2)}/${numeros.substring(2, 4)}/${numeros.substring(4, 8)}"
    } else {
        data
    }
}