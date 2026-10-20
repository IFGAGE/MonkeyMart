package com.example.marketplace.ui.screens.negociante

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.layout.ContentScale

import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.marketplace.data.local.GerenciadorDeFotos
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroProdutoScreen(
    onSalvar: (nome: String, descricao: String, preco: String, fotoPathLocal: String?) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val gerenciadorDeFotos = remember { GerenciadorDeFotos(context) }

    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }

    var arquivoDaFotoAtual by remember { mutableStateOf<File?>(null) }
    var uriDaFotoLocal by remember { mutableStateOf<Uri?>(null) }

    val disparadorTirarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->
        if (sucesso) {
            uriDaFotoLocal = arquivoDaFotoAtual?.let { Uri.fromFile(it) }
        }
    }

    val disparadorEscolherImagem = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { origem ->
            val (destino, _) = gerenciadorDeFotos.criarArquivoDeImagem()
            context.contentResolver.openInputStream(origem)?.use { entrada ->
                destino.outputStream().use { saida -> entrada.copyTo(saida) }
            }
            arquivoDaFotoAtual = destino
            uriDaFotoLocal = Uri.fromFile(destino)
        }
    }

    val pedirPermissaoCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            val (arquivo, uri) = gerenciadorDeFotos.criarArquivoDeImagem()
            arquivoDaFotoAtual = arquivo
            disparadorTirarFoto.launch(uri)
        }
    }

    fun abrirCamera() {
        val permissaoConcedida = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permissaoConcedida) {
            val (arquivo, uri) = gerenciadorDeFotos.criarArquivoDeImagem()
            arquivoDaFotoAtual = arquivo
            disparadorTirarFoto.launch(uri)
        } else {
            pedirPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Produto") },
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
            // Área de foto — campo com placeholder estilizado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uriDaFotoLocal != null) {
                    AsyncImage(
                        model = uriDaFotoLocal,
                        contentDescription = "Foto do produto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Adicionar foto do produto",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { abrirCamera() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Câmera")
                }
                OutlinedButton(
                    onClick = {
                        disparadorEscolherImagem.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galeria")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Produto") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = preco,
                onValueChange = { preco = it },
                label = { Text("Preço (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSalvar(nome, descricao, preco, arquivoDaFotoAtual?.absolutePath) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Produto")
            }
        }
    }
}