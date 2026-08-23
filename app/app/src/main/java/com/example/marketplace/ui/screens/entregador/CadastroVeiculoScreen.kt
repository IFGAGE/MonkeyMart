package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroVeiculoScreen(
    // Agora devolvemos os 4 textos preenchidos
    onSalvar: (placa: String, modelo: String, marca: String, ano: String) -> Unit,
    onCancelar: () -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var ano by remember { mutableStateOf("") } // Adicionado para bater com o banco

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar Veículo") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
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
                .fillMaxSize()
        ) {
            OutlinedTextField(value = placa, onValueChange = { placa = it.uppercase() }, label = { Text("Placa") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo (Ex: CG 160)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca (Ex: Honda)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ano,
                onValueChange = { ano = it },
                label = { Text("Ano") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSalvar(placa, modelo, marca, ano) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Veículo")
            }
        }
    }
}