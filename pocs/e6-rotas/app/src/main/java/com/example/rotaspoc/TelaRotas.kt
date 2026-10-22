package com.example.rotaspoc

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaRotas(model: RotasViewModel) {
    val estado by model.estado.collectAsState()
    val context = LocalContext.current
    val foco = LocalFocusManager.current
    val quantidade = pontosValidos(estado.campos).size
    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
        if (resultados.values.any { it }) model.gerarRota() else model.permissaoNegada()
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Geração de rotas") }) }, bottomBar = {
        Surface(tonalElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("$quantidade endereços localizados · mínimo de 2", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    foco.clearFocus()
                    val concedida = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                    if (concedida) model.gerarRota()
                    else permissao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, enabled = quantidade >= 2 && !estado.gerando, modifier = Modifier.fillMaxWidth()) {
                    Text("Gerar rota")
                }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Quais serão as paradas?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Digite endereços no Brasil. Confira cada ponto no mapa antes de gerar a rota.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            itemsIndexed(estado.campos, key = { _, campo -> campo.id }) { indice, campo ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Parada ${indice + 1}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (estado.campos.size > 2) TextButton(onClick = { model.removerParada(campo.id) }) { Text("Remover") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = campo.texto, onValueChange = { model.alterarEndereco(campo.id, it) },
                                label = { Text("Endereço") }, placeholder = { Text("Rua, número, cidade e estado") },
                                isError = campo.erro != null, modifier = Modifier.weight(1f), maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }))
                            if (campo.ponto != null) OutlinedButton(onClick = { foco.clearFocus(); model.verEndereco(campo.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) { Text("Ver no mapa") }
                        }
                        if (campo.buscando) Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Localizando endereço…", style = MaterialTheme.typography.bodySmall)
                        }
                        campo.erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        if (campo.ponto != null) Text("Endereço localizado", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                OutlinedButton(onClick = model::adicionarParada, modifier = Modifier.fillMaxWidth()) { Text("+ Adicionar parada") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    estado.painel?.let { painel -> PainelMapa(painel, model::fecharPainel) }
}

@Composable
private fun PainelMapa(painel: Painel, fechar: () -> Unit) {
    Dialog(onDismissRequest = fechar, properties = DialogProperties(
        dismissOnClickOutside = false, dismissOnBackPress = true, usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.90f),
            shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (painel is Painel.Endereco) "Conferir endereço" else "Sua rota",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = fechar) { Text("Fechar") }
                }
                when (painel) {
                    is Painel.Gerando -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator()
                            Text(painel.mensagem)
                        }
                    }
                    is Painel.Erro -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(painel.mensagem, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = fechar, modifier = Modifier.fillMaxWidth()) { Text("Voltar aos endereços") }
                    }
                    is Painel.Endereco -> {
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                            val alturaMapa = maxOf(220.dp, maxHeight * 0.7f)
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(painel.ponto.endereco, style = MaterialTheme.typography.bodyMedium)
                                Mapa(pontos = listOf(painel.ponto), modifier = Modifier.fillMaxWidth().height(alturaMapa))
                                Text("Confira se o ponto corresponde ao endereço informado.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is Painel.Gerada -> {
                        val rota = painel.rota
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                            val alturaMapa = maxOf(220.dp, maxHeight * 0.65f)
                            // Rolagem simples mantém o mapa composto; a altura mínima
                            // evita que os textos eliminem o mapa em paisagem.
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(String.format(Locale("pt", "BR"), "%.1f km · %d min · %d paradas",
                                    rota.distanciaMetros / 1000, ceil(rota.duracaoSegundos / 60).toInt(), rota.paradas.size),
                                    style = MaterialTheme.typography.titleMedium)
                                Mapa(rota.paradas, rota.origem, rota.geometria, Modifier.fillMaxWidth().height(alturaMapa))
                                Text(if (rota.paradas.size <= 12) "Paradas ordenadas pelo menor tempo de viagem." else
                                    "Ordem aproximada pela parada mais próxima.", style = MaterialTheme.typography.bodySmall)
                                if (painel.ignorados > 0) Text("${painel.ignorados} campo(s) sem validação ficaram fora da rota.",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Origem: sua localização atual", fontWeight = FontWeight.SemiBold)
                                rota.paradas.forEachIndexed { indice, ponto ->
                                    Text("${indice + 1}. ${ponto.endereco}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
