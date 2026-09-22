package com.example.storage

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var gerenciadorDeFotos: GerenciadorDeFotos
    private val repositorio = ConexaoFirebaseStorage()

    private var arquivoDaFotoAtual: File? = null
    private var uriDaFotoLocal by mutableStateOf<Uri?>(null)
    private var statusDaSincronizacao by mutableStateOf("—")

    private val disparadorTirarFoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
            if (sucesso) {
                uriDaFotoLocal = arquivoDaFotoAtual?.let { Uri.fromFile(it) }
                arquivoDaFotoAtual?.let { sincronizarComFirebase(it) }
            }
        }

    private val disparadorEscolherImagem =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { copiarParaArmazenamentoLocal(it) }
        }

    private val pedirPermissaoCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (concedida) {
                abrirCameraInterno()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gerenciadorDeFotos = GerenciadorDeFotos(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = { abrirCamera() }) {
                            Text("Tirar foto")
                        }
                        Button(onClick = { abrirGaleria() }) {
                            Text("Escolher da galeria")
                        }

                        uriDaFotoLocal?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto local",
                                modifier = Modifier.size(200.dp)
                            )
                        }

                        Text("Status da sincronização: $statusDaSincronizacao")
                    }
                }
            }
        }
    }

    private fun abrirCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                abrirCameraInterno()
            }
            else -> {
                pedirPermissaoCamera.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCameraInterno() {
        val (arquivo, uri) = gerenciadorDeFotos.criarArquivoDeImagem()
        arquivoDaFotoAtual = arquivo
        disparadorTirarFoto.launch(uri)
    }

    private fun abrirGaleria() {
        disparadorEscolherImagem.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun copiarParaArmazenamentoLocal(uriDeOrigem: Uri) {
        val (arquivoDestino, _) = gerenciadorDeFotos.criarArquivoDeImagem()
        contentResolver.openInputStream(uriDeOrigem)?.use { entrada ->
            arquivoDestino.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        arquivoDaFotoAtual = arquivoDestino
        uriDaFotoLocal = Uri.fromFile(arquivoDestino)
        sincronizarComFirebase(arquivoDestino)
    }

    private fun sincronizarComFirebase(arquivo: File) {
        statusDaSincronizacao = "Enviando..."
        lifecycleScope.launch {
            val resultado = repositorio.enviarFoto(arquivo, idDoItem = "produto_teste_poc")
            resultado.onSuccess { url ->
                statusDaSincronizacao = "Sincronizado ✅"
                Log.d("SyncPhoto", "Upload concluído: $url")
            }.onFailure { erro ->
                statusDaSincronizacao = "Falhou (mantido local) ⚠️"
                Log.e("SyncPhoto", "Falha no upload", erro)
            }
        }
    }
}