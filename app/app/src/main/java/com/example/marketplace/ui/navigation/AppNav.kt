package com.example.marketplace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState // <-- Importante para ler do banco em tempo real!
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Imports do Banco de Dados
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.UsuarioEntity
import com.example.marketplace.data.entity.VeiculoEntity

// Imports das Telas
import com.example.marketplace.ui.screens.auth.LoginScreen
import com.example.marketplace.ui.screens.profile.ProfileSelectionScreen
import com.example.marketplace.ui.screens.negociante.NegocianteHomeScreen
import com.example.marketplace.ui.screens.negociante.CadastroProdutoScreen
import com.example.marketplace.ui.screens.negociante.ProdutoTemp
import com.example.marketplace.ui.screens.entregador.EntregadorHomeScreen
import com.example.marketplace.ui.screens.entregador.CadastroVeiculoScreen
import com.example.marketplace.ui.screens.profile.ProfileScreen
import com.example.marketplace.ui.screens.profile.UsuarioTemp

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    val startDestination = if (auth.currentUser != null) "check_profile" else "login"

    var usuarioLogado by remember { mutableStateOf<UsuarioTemp?>(null) }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("check_profile") {
            LaunchedEffect(Unit) {
                val email = auth.currentUser?.email
                if (email != null) {
                    val userInDb = db.usuarioDao().buscarUsuario(email)

                    if (userInDb != null) {
                        usuarioLogado = UsuarioTemp(
                            nome = userInDb.nome,
                            telefone = userInDb.telefone,
                            cpf = userInDb.cpf,
                            dataNascimento = userInDb.dataNascimento,
                            tipoPerfil = userInDb.tipoPerfil
                        )
                        val rota = if (userInDb.tipoPerfil == "negociante") "area_negociante" else "area_entregador"
                        navController.navigate(rota) { popUpTo("check_profile") { inclusive = true } }
                    } else {
                        navController.navigate("profile_selection") { popUpTo("check_profile") { inclusive = true } }
                    }
                } else {
                    navController.navigate("login") { popUpTo("check_profile") { inclusive = true } }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("check_profile") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("profile_selection") {
            ProfileSelectionScreen(
                onCadastroConcluido = { usuario ->
                    usuarioLogado = usuario
                    val email = auth.currentUser?.email ?: "email_desconhecido"

                    coroutineScope.launch {
                        val novoUsuarioEntity = UsuarioEntity(
                            email = email,
                            nome = usuario.nome,
                            telefone = usuario.telefone,
                            cpf = usuario.cpf,
                            dataNascimento = usuario.dataNascimento,
                            tipoPerfil = usuario.tipoPerfil
                        )
                        db.usuarioDao().salvarUsuario(novoUsuarioEntity)

                        val rotaDestino = if (usuario.tipoPerfil == "negociante") "area_negociante" else "area_entregador"
                        navController.navigate(rotaDestino) {
                            popUpTo(0)
                        }
                    }
                },
                onSignOut = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        // --- ÁREA DO NEGOCIANTE ---
        composable("area_negociante") {
            val email = auth.currentUser?.email ?: ""
            val produtosList by db.produtoDao().buscarProdutosDoNegociante(email).collectAsState(initial = emptyList())

            // Agora o ProdutoTemp recebe os dados novos!
            val produtosVisuais = produtosList.map {
                ProdutoTemp(it.nome, it.preco, it.descricao, it.nomeDono, it.fotoPathLocal)
            }

            NegocianteHomeScreen(
                produtos = produtosVisuais,
                onAddProdutoClick = { navController.navigate("cadastro_produto") },
                onProfileClick = { navController.navigate("perfil") }
            )
        }

        composable("cadastro_produto") {
            CadastroProdutoScreen(
                onSalvar = { nome, descricao, preco ->
                    val email = auth.currentUser?.email ?: ""
                    // Pega o nome do usuário logado na RAM, se não tiver, põe Desconhecido
                    val nomeVendedor = usuarioLogado?.nome ?: "Desconhecido"

                    coroutineScope.launch {
                        val novoProduto = ProdutoEntity(
                            emailDono = email,
                            nomeDono = nomeVendedor,
                            nome = nome,
                            descricao = descricao,
                            preco = preco,
                            fotoPathLocal = "" // Fica vazio até implementarmos a câmera
                        )
                        db.produtoDao().salvarProduto(novoProduto)
                    }
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        // --- ÁREA DO ENTREGADOR ---
        composable("area_entregador") {
            // LER DO BANCO: Entregador vê TODOS os produtos (Futuramente mudaremos para Pedidos!)
            val todosProdutos by db.produtoDao().buscarTodosProdutos().collectAsState(initial = emptyList())

            // CORREÇÃO AQUI: Passando os 5 parâmetros exigidos pelo ProdutoTemp
            val produtosVisuais = todosProdutos.map {
                ProdutoTemp(it.nome, it.preco, it.descricao, it.nomeDono, it.fotoPathLocal)
            }

            EntregadorHomeScreen(
                produtos = produtosVisuais,
                onAddVeiculoClick = { navController.navigate("cadastro_veiculo") },
                onProfileClick = { navController.navigate("perfil") }
            )
        }

        composable("cadastro_veiculo") {
            CadastroVeiculoScreen(
                onSalvar = { placa, modelo, marca, ano -> // <-- ERRO CORRIGIDO AQUI!
                    val email = auth.currentUser?.email ?: ""

                    coroutineScope.launch {
                        val novoVeiculo = VeiculoEntity(
                            placa = placa,
                            emailDono = email,
                            modelo = modelo,
                            marca = marca,
                            ano = ano
                        )
                        db.veiculoDao().salvarVeiculo(novoVeiculo) // Salva no SQLite!
                    }
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        // --- TELA DE PERFIL ---
        composable("perfil") {
            ProfileScreen(
                userEmail = auth.currentUser?.email ?: "Usuário",
                usuario = usuarioLogado,
                onSignOut = {
                    coroutineScope.launch {
                        db.usuarioDao().deletarTudo() // Limpa na hora de sair
                    }
                    auth.signOut()
                    usuarioLogado = null
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}