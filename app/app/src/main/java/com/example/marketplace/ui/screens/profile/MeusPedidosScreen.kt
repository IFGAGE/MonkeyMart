package com.example.marketplace.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modelo visual específico para o cliente
data class PedidoCliente(
    val id: Int,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusPedidosScreen(
    pedidos: List<PedidoCliente>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Pedidos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Voltar") }
                }
            )
        }
    ) { paddingValues ->
        if (pedidos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Você ainda não fez nenhum pedido.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
                items(pedidos) { pedido ->
                    PedidoClienteCard(pedido)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoClienteCard(pedido: PedidoCliente) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Pedido #${pedido.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "R$ ${pedido.valorTotal}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Mostra o status com cor dinâmica
            Text(
                text = "Status: ${pedido.statusEntrega}", 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold,
                color = if (pedido.statusEntrega == "ENTREGUE") Color(0xFF4CAF50) else Color(0xFFFFA000)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Place, contentDescription = "Endereço", modifier = Modifier.size(20.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = pedido.enderecoEntrega, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = "Detalhes do Pedido #${pedido.id}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Status Atual", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray)
                Text(
                    text = pedido.statusEntrega, 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pedido.statusEntrega == "ENTREGUE") Color(0xFF4CAF50) else Color(0xFFFFA000)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Endereço de Entrega", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray)
                Text(text = pedido.enderecoEntrega, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Resumo dos Itens", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray)
                Text(text = pedido.resumoItens, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total pago:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "R$ ${pedido.valorTotal}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}