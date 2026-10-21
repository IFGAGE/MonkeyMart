package com.example.marketplace.ui.screens.negociante

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.layout.ContentScale

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

    var nome by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var preco by rememberSaveable { mutableStateOf("") }

    var fotoPathLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var capturaPendentePath by rememberSaveable { mutableStateOf<String?>(null) }
    var erroFoto by remember { mutableStateOf<String?>(null) }
    val uriDaFotoLocal = fotoPathLocal?.let { Uri.fromFile(File(it)) }

    val disparadorImagem = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val captura = capturaPendentePath?.let(::File)
        try {
            if (resultado.resultCode == Activity.RESULT_OK) {
                if (captura != null && captura.length() > 0) {
                    fotoPathLocal = captura.absolutePath
                } else {
                    val origem = checkNotNull(resultado.data?.data)
                    val (destino, _) = gerenciadorDeFotos.criarArquivoDeImagem()
                    try {
                        checkNotNull(context.contentResolver.openInputStream(origem)).use { entrada ->
                            destino.outputStream().use { saida -> entrada.copyTo(saida) }
                        }
                        check(destino.length() > 0)
                        fotoPathLocal = destino.absolutePath
                    } finally {
                        if (destino.absolutePath != fotoPathLocal) destino.delete()
                    }
                }
            }
        } catch (e: Exception) {
            erroFoto = "Não foi possível adicionar a foto. Tente novamente."
        } finally {
            if (captura?.absolutePath != fotoPathLocal) captura?.delete()
            capturaPendentePath = null
        }
    }

    fun escolherFoto() {
        erroFoto = null
        try {
            val (arquivo, uri) = gerenciadorDeFotos.criarArquivoDeImagem()
            capturaPendentePath = arquivo.absolutePath
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                clipData = ClipData.newRawUri("Foto do produto", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val galeria = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            val escolha = Intent.createChooser(galeria, "Adicionar foto do produto").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
            }
            disparadorImagem.launch(escolha)
        } catch (e: Exception) {
            capturaPendentePath?.let { File(it).delete() }
            capturaPendentePath = null
            erroFoto = "Não foi possível abrir o seletor de imagens."
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
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Área de foto — campo com placeholder estilizado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (fotoPathLocal == null) "Adicionar foto do produto" else "Trocar foto do produto",
                        onClick = { escolherFoto() }
                    )
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

            erroFoto?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
                onClick = { onSalvar(nome, descricao, preco, fotoPathLocal) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Salvar Produto")
            }
        }
    }
}
