package com.example.marketplace.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class GerenciadorDeFotos(private val contexto: Context) {

    fun criarArquivoDeImagem(): Pair<File, Uri> {
        val diretorioImagens = File(contexto.filesDir, "imagens").apply { mkdirs() }
        val nomeDoArquivo = "IMG_${System.currentTimeMillis()}.jpg"
        val arquivo = File(diretorioImagens, nomeDoArquivo)
        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            arquivo
        )
        return arquivo to uri
    }
}