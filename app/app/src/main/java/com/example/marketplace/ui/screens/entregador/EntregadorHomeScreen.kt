package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketplace.R
import com.example.marketplace.ui.screens.profile.VeiculoTemp

data class PedidoTemp(
    val id: Int,
    val emailCliente: String,
    val enderecoEntrega: String,
    val resumoItens: String,
    val valorTotal: String,
    val statusEntrega: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregadorHomeScreen(
    pedidos: List<PedidoTemp>,
    veiculos: List<VeiculoTemp>,
    onAddVeiculoClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMarcarComoEntregue: (Int) -> Unit,
    onVerMapaClick: (Int, String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val logo = if (isDarkTheme) R.drawable.mm_branco else R.drawable.mm_preto

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = logo),
                        contentDescription = null,
                        modifier = Modifier.height(40.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAddVeiculoClick) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
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
                text = "Pedidos Disponíveis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            if (pedidos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum pedido pendente no momento.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pedidos) { pedido ->
                        PedidoItemCard(
                            pedido = pedido,
                            veiculos = veiculos,
                            onMarcarComoEntregue = onMarcarComoEntregue,
                            onVerMapaClick = onVerMapaClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoItemCard(
    pedido: PedidoTemp,
    veiculos: List<VeiculoTemp>,
    onMarcarComoEntregue: (Int) -> Unit,
    onVerMapaClick: (Int, String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var veiculoSelecionado by remember { mutableStateOf<VeiculoTemp?>(null) }

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

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = pedido.enderecoEntrega, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                veiculoSelecionado = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(text = "Detalhes do Pedido #${pedido.id}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Endereço de Entrega", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray)
                Text(text = pedido.enderecoEntrega, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onVerMapaClick(pedido.id, pedido.enderecoEntrega)
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visualizar no Mapa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Resumo dos Itens", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray)
                Text(text = pedido.resumoItens, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Selecione o veículo da entrega:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (veiculos.isEmpty()) {
                    Text(
                        text = "Você precisa cadastrar um veículo antes de entregar.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    veiculos.forEach { veiculo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { veiculoSelecionado = veiculo }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (veiculoSelecionado == veiculo),
                                onClick = { veiculoSelecionado = veiculo }
                            )
                            Text(text = "${veiculo.modelo} (${veiculo.placa})")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onMarcarComoEntregue(pedido.id)
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = veiculoSelecionado != null
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marcar como Entregue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}