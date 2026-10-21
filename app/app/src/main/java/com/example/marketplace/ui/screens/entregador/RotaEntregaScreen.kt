package com.example.marketplace.ui.screens.entregador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketplace.data.entity.RotaEntity
import com.example.marketplace.ui.mapa.MapaRota
import com.example.marketplace.ui.mapa.RotaGerada
import com.example.marketplace.ui.screens.profile.VeiculoTemp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotaEntregaScreen(
    rota: RotaEntity,
    rotaGerada: RotaGerada,
    pedidos: List<PedidoTemp>,
    veiculos: List<VeiculoTemp>,
    processando: Boolean,
    erro: String?,
    onVoltar: () -> Unit,
    onCadastrarVeiculo: () -> Unit,
    onExcluir: () -> Unit,
    onIniciar: (String) -> Unit,
    onAlternarEntrega: (PedidoTemp) -> Unit,
    onCancelar: () -> Unit,
    onFinalizar: () -> Unit
) {
    var placaSelecionada by rememberSaveable(rota.emailEntregador) { mutableStateOf(rota.placaVeiculo) }
    val iniciada = rota.estado == "INICIADA"
    val todosEntregues = pedidos.size == rotaGerada.paradas.size &&
        pedidos.isNotEmpty() && pedidos.all { it.statusEntrega == "ENTREGUE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (iniciada) "Rota iniciada" else "Rota planejada") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MapaRota(
                    rota = rotaGerada,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }

            if (!iniciada) {
                item {
                    Text("Selecione o veículo", style = MaterialTheme.typography.titleMedium)
                    if (veiculos.isEmpty()) {
                        Text("Cadastre um veículo antes de iniciar a rota.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onCadastrarVeiculo) { Text("Cadastrar veículo") }
                    } else {
                        veiculos.forEach { veiculo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { placaSelecionada = veiculo.placa }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = placaSelecionada == veiculo.placa,
                                    onClick = { placaSelecionada = veiculo.placa }
                                )
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("${veiculo.marca} ${veiculo.modelo} (${veiculo.placa})")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("Veículo: ${rota.placaVeiculo}", fontWeight = FontWeight.Bold)
                }
            }

            item { Text("Pedidos da rota", style = MaterialTheme.typography.titleMedium) }

            items(pedidos, key = { it.id }) { pedido ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iniciada) {
                            Checkbox(
                                checked = pedido.statusEntrega == "ENTREGUE",
                                onCheckedChange = { onAlternarEntrega(pedido) },
                                enabled = !processando
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pedido #${pedido.id}", fontWeight = FontWeight.Bold)
                            Text(pedido.enderecoEntrega)
                            if (iniciada) Text(if (pedido.statusEntrega == "ENTREGUE") "Entregue" else "Pendente")
                        }
                    }
                }
            }

            erro?.let { mensagem ->
                item { Text(mensagem, color = MaterialTheme.colorScheme.error) }
            }

            item {
                if (iniciada) {
                    Button(
                        onClick = onFinalizar,
                        enabled = todosEntregues && !processando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finalizar rota")
                    }
                    OutlinedButton(
                        onClick = onCancelar,
                        enabled = !processando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar rota")
                    }
                } else {
                    Button(
                        onClick = { placaSelecionada?.let(onIniciar) },
                        enabled = placaSelecionada != null && !processando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Iniciar rota")
                    }
                    OutlinedButton(
                        onClick = onExcluir,
                        enabled = !processando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Excluir rota")
                    }
                }
            }
        }
    }
}
