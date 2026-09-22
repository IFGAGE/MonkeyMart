package com.example.storage

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File

class ConexaoFirebaseStorage {

    private val referenciaStorage = Firebase.storage.reference

    suspend fun enviarFoto(arquivoLocal: File, idDoItem: String): Result<String> {
        return try {
            val uriDoArquivo = Uri.fromFile(arquivoLocal)
            val referenciaRemota = referenciaStorage.child("produtos/$idDoItem/${arquivoLocal.name}")

            referenciaRemota.putFile(uriDoArquivo).await()
            val urlDeDownload = referenciaRemota.downloadUrl.await()

            Result.success(urlDeDownload.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}