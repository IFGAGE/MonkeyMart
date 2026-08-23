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
    usuario: UsuarioTemp?, // <-- Recebe os dados completos do usuário
    onSignOut: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
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

            // Verifica se tem os dados e exibe
            if (usuario != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(label = "Nome:", value = usuario.nome)
                        InfoRow(label = "E-mail:", value = userEmail)
                        InfoRow(label = "CPF:", value = usuario.cpf)
                        InfoRow(label = "Telefone:", value = usuario.telefone)
                        InfoRow(label = "Nascimento:", value = usuario.dataNascimento)
                        InfoRow(label = "Perfil:", value = usuario.tipoPerfil.replaceFirstChar { it.uppercase() })
                    }
                }
            } else {
                // Caso a memória zere (fallback preventivo)
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

            Spacer(modifier = Modifier.weight(1f)) // Empurra o botão pra baixo

            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sair da conta", fontSize = 16.sp)
            }
        }
    }
}

// Componente reutilizável para exibir cada linha de informação
@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}