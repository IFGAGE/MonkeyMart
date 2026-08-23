package com.example.marketplace.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketplace.ui.theme.ColoredLinks
import com.example.marketplace.ui.utils.MaskVisualTransformation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ProfileSelectionScreen(
    onCadastroConcluido: (UsuarioTemp) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedProfile by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selectedProfile == null) {
            // --- ESTADO 1: ESCOLHA DE PERFIL ---
            Text(
                text = "Escolha um perfil para realizar o cadastro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileCard("Negociante", Icons.Default.ShoppingCart) { selectedProfile = "negociante" }
                ProfileCard("Entregador", Icons.Default.Person) { selectedProfile = "entregador" }
            }
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = onSignOut) {
                Text("Sair da conta", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
            }
        } else {
            // --- ESTADO 2: FORMULÁRIO DE CADASTRO ---
            FormularioCadastro(
                perfil = selectedProfile!!,
                onVoltarClick = { selectedProfile = null },
                onSalvarClick = { usuario ->
                    onCadastroConcluido(usuario)
                }
            )
        }
    }
}

@Composable
fun FormularioCadastro(
    perfil: String,
    onVoltarClick: () -> Unit,
    onSalvarClick: (UsuarioTemp) -> Unit // <-- CORREÇÃO: Agora recebe o UsuarioTemp
) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Complete seu cadastro", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = "Perfil: ${perfil.replaceFirstChar { it.uppercase() }}", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome Completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- TELEFONE COM MÁSCARA ---
        val mascaraTelefone = if (telefone.length <= 10) "(##) ####-####" else "(##) #####-####"

        OutlinedTextField(
            value = telefone,
            onValueChange = {
                val digits = it.filter { char -> char.isDigit() }
                telefone = digits.take(11)
            },
            label = { Text("Telefone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = MaskVisualTransformation(mascaraTelefone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- CPF COM MÁSCARA ---
        OutlinedTextField(
            value = cpf,
            onValueChange = {
                val digits = it.filter { char -> char.isDigit() }
                cpf = digits.take(11)
            },
            label = { Text("CPF") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = MaskVisualTransformation("###.###.###-##"),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- DATA DE NASCIMENTO COM MÁSCARA ---
        OutlinedTextField(
            value = dataNascimento,
            onValueChange = {
                val digits = it.filter { char -> char.isDigit() }
                dataNascimento = digits.take(8)
            },
            label = { Text("Data de Nascimento") },
            placeholder = { Text("Ex: 05/05/2005") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = MaskVisualTransformation("##/##/####"),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nome.isBlank() || telefone.isBlank() || cpf.isBlank() || dataNascimento.isBlank()) {
                    Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                try {
                    val formatter = DateTimeFormatter.ofPattern("ddMMyyyy")
                    val birthDate = LocalDate.parse(dataNascimento, formatter)
                    val age = ChronoUnit.YEARS.between(birthDate, LocalDate.now())

                    if (perfil == "entregador" && age < 18) {
                        Toast.makeText(context, "Entregadores precisam ter 18 anos ou mais", Toast.LENGTH_LONG).show()
                    } else if (perfil == "negociante" && age < 14) {
                        Toast.makeText(context, "Negociantes precisam ter 14 anos ou mais", Toast.LENGTH_LONG).show()
                    } else {
                        // <-- CORREÇÃO: Instancia o usuário e envia!
                        val novoUsuario = UsuarioTemp(nome, telefone, cpf, dataNascimento, perfil)
                        onSalvarClick(novoUsuario)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Data inválida.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Finalizar Cadastro", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onVoltarClick) {
            Text("Voltar para escolha de perfil", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ProfileCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(150.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(56.dp), tint = ColoredLinks)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    }
}

data class UsuarioTemp(
    val nome: String,
    val telefone: String,
    val cpf: String,
    val dataNascimento: String,
    val tipoPerfil: String
)