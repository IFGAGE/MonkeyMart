package com.example.marketplace.data.sync

import com.example.marketplace.data.dao.UsuarioDao
import com.example.marketplace.data.entity.EnderecoColeta
import com.example.marketplace.data.entity.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

internal suspend fun UsuarioDao.buscarOuRestaurarPerfil(email: String): UsuarioEntity? {
    buscarUsuario(email)?.let { return it }

    val perfil = FirebaseFirestore.getInstance().collection("usuarios").document(email)
        .get(Source.SERVER).await()
    if (!perfil.exists()) return null

    val tipo = perfil.getString("tipoPerfil")
    require(tipo == "negociante" || tipo == "entregador") { "Perfil inválido." }

    val coleta = (perfil.get("enderecoColeta") as? Map<*, *>)?.let { campos ->
        fun texto(campo: String) = campos[campo] as? String ?: ""
        EnderecoColeta(
            cep = texto("cep"), rua = texto("rua"), numero = texto("numero"),
            complemento = texto("complemento"), bairro = texto("bairro"), cidade = texto("cidade"),
            uf = texto("uf"), endereco = texto("endereco"),
            latitude = (campos["latitude"] as? Number)?.toDouble(),
            longitude = (campos["longitude"] as? Number)?.toDouble()
        )
    }
    return UsuarioEntity(
        email = email,
        nome = perfil.getString("nome").orEmpty(),
        telefone = perfil.getString("telefone").orEmpty(),
        cpf = perfil.getString("cpf").orEmpty(),
        dataNascimento = perfil.getString("dataNascimento").orEmpty(),
        tipoPerfil = tipo,
        isSynced = true,
        enderecoColeta = coleta
    ).also { salvarUsuario(it) }
}
