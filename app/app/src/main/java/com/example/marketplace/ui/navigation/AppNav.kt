package com.example.marketplace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Imports do Banco de Dados
import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.AvaliacaoEntity
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.UsuarioEntity
import com.example.marketplace.data.entity.VeiculoEntity

// Imports das Telas
import com.example.marketplace.ui.screens.auth.LoginScreen
import com.example.marketplace.ui.screens.profile.ProfileSelectionScreen
import com.example.marketplace.ui.screens.negociante.NegocianteHomeScreen
import com.example.marketplace.ui.screens.negociante.CadastroProdutoScreen
import com.example.marketplace.ui.screens.negociante.ProdutoTemp
import com.example.marketplace.ui.screens.negociante.ItemCarrinho
import com.example.marketplace.ui.screens.negociante.CarrinhoScreen
import com.example.marketplace.ui.screens.entregador.EntregadorHomeScreen
import com.example.marketplace.ui.screens.entregador.CadastroVeiculoScreen
import com.example.marketplace.ui.screens.profile.ProfileScreen
import com.example.marketplace.ui.screens.profile.UsuarioTemp

@Composable
fun AppNavigation(
    // 1. Injetamos o ViewModel aqui! Ele sobrevive a mudanças de tema e rotação de tela.
    viewModel: AppViewModel = viewModel()
) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    val startDestination = if (auth.currentUser != null) "check_profile" else "login"

    // 2. Agora pegamos os itens direto do "Cofre" (ViewModel)
    val carrinhoItens = viewModel.carrinhoItens

    NavHost(navController = navController, startDestination = startDestination) {

        composable("check_profile") {
            LaunchedEffect(Unit) {
                val email = auth.currentUser?.email
                if (email != null) {
                    val userInDb = db.usuarioDao().buscarUsuario(email)

                    if (userInDb != null) {
                        viewModel.usuarioLogado = UsuarioTemp(
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("check_profile") { popUpTo("login") { inclusive = true } } }
            )
        }

        composable("profile_selection") {
            ProfileSelectionScreen(
                onCadastroConcluido = { usuario ->
                    viewModel.usuarioLogado = usuario
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
                        navController.navigate(rotaDestino) { popUpTo(0) }
                    }
                },
                onSignOut = {
                    auth.signOut()
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }

        // --- ÁREA DO NEGOCIANTE ---
        composable("area_negociante") {
            val email = auth.currentUser?.email ?: ""
            val produtosList by db.produtoDao().buscarTodosProdutos().collectAsState(initial = emptyList())

            val pedidosEntregues by db.pedidoDao().buscarPedidosEntregues(email).collectAsState(initial = emptyList())
            val historicoComprasEntregues = pedidosEntregues.joinToString { it.resumoItens }

            val avaliacoesGerais by db.avaliacaoDAO().buscarTodas().collectAsState(initial = emptyList())

            val produtosVisuais = produtosList.map { produtoEntity ->
                val avaliacoesDoProduto = avaliacoesGerais
                    .filter { it.nomeProduto == produtoEntity.nome }
                    .map { com.example.marketplace.ui.screens.negociante.AvaliacaoTemp(it.nota, it.comentario, it.emailAutor) }

                val jaAvaliou = avaliacoesDoProduto.any { it.emailAutor == email }

                ProdutoTemp(
                    nome = produtoEntity.nome,
                    preco = produtoEntity.preco,
                    descricao = produtoEntity.descricao,
                    nomeDono = produtoEntity.nomeDono,
                    fotoPathLocal = produtoEntity.fotoPathLocal,
                    avaliacoes = avaliacoesDoProduto,
                    podeAvaliar = historicoComprasEntregues.contains(produtoEntity.nome) && !jaAvaliou
                )
            }

            NegocianteHomeScreen(
                produtos = produtosVisuais,
                quantidadeCarrinho = carrinhoItens.sumOf { it.quantidade },
                onAddProdutoClick = { navController.navigate("cadastro_produto") },
                onProfileClick = { navController.navigate("perfil") },
                onCartClick = { navController.navigate("carrinho") },
                onAddToCart = { produtoSelecionado ->
                    val itemExistente = carrinhoItens.find { it.produto.nome == produtoSelecionado.nome }
                    if (itemExistente != null) {
                        val index = carrinhoItens.indexOf(itemExistente)
                        carrinhoItens[index] = itemExistente.copy(quantidade = itemExistente.quantidade + 1)
                    } else {
                        carrinhoItens.add(ItemCarrinho(produtoSelecionado))
                    }
                },
                onEnviarAvaliacao = { nomeProduto, nota, comentario ->
                    coroutineScope.launch {
                        val novaAvaliacao = AvaliacaoEntity(
                            nomeProduto = nomeProduto,
                            emailAutor = email,
                            nota = nota,
                            comentario = comentario
                        )
                        db.avaliacaoDAO().salvarAvaliacao(novaAvaliacao)
                    }
                }
            )
        }

        // --- TELA DO CARRINHO ---
        composable("carrinho") {
            CarrinhoScreen(
                itensCarrinho = carrinhoItens,
                onAumentar = { item ->
                    val index = carrinhoItens.indexOf(item)
                    carrinhoItens[index] = item.copy(quantidade = item.quantidade + 1)
                },
                onDiminuir = { item ->
                    val index = carrinhoItens.indexOf(item)
                    carrinhoItens[index] = item.copy(quantidade = item.quantidade - 1)
                },
                onRemover = { item ->
                    carrinhoItens.remove(item)
                },
                onVoltar = { navController.popBackStack() },
                onFinalizarPedido = { endereco, total ->
                    val email = auth.currentUser?.email ?: "cliente_desconhecido"

                    val resumoText = carrinhoItens.joinToString(separator = ", ") {
                        "${it.quantidade}x ${it.produto.nome}"
                    }

                    coroutineScope.launch {
                        val novoPedido = PedidoEntity(
                            emailCliente = email,
                            enderecoEntrega = endereco,
                            resumoItens = resumoText,
                            valorTotal = total,
                            statusEntrega = "PENDENTE"
                        )
                        db.pedidoDao().salvarPedido(novoPedido)
                        carrinhoItens.clear()
                    }
                    navController.popBackStack()
                }
            )
        }

        composable("cadastro_produto") {
            CadastroProdutoScreen(
                onSalvar = { nome, descricao, preco ->
                    val email = auth.currentUser?.email ?: ""
                    val nomeVendedor = viewModel.usuarioLogado?.nome ?: "Desconhecido"

                    coroutineScope.launch {
                        val novoProduto = ProdutoEntity(
                            emailDono = email,
                            nomeDono = nomeVendedor,
                            nome = nome,
                            descricao = descricao,
                            preco = preco,
                            fotoPathLocal = ""
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
            val email = auth.currentUser?.email ?: ""

            val pedidosPendentes by db.pedidoDao().buscarPedidosPendentes().collectAsState(initial = emptyList())
            val pedidosVisuais = pedidosPendentes.map {
                com.example.marketplace.ui.screens.entregador.PedidoTemp(
                    id = it.id,
                    emailCliente = it.emailCliente,
                    enderecoEntrega = it.enderecoEntrega,
                    resumoItens = it.resumoItens,
                    valorTotal = it.valorTotal,
                    statusEntrega = it.statusEntrega
                )
            }

            val veiculosList by db.veiculoDao().buscarVeiculosDoEntregador(email).collectAsState(initial = emptyList())
            val veiculosVisuais = veiculosList.map {
                com.example.marketplace.ui.screens.profile.VeiculoTemp(it.placa, it.modelo, it.marca, it.ano)
            }

            EntregadorHomeScreen(
                pedidos = pedidosVisuais,
                veiculos = veiculosVisuais,
                onAddVeiculoClick = { navController.navigate("cadastro_veiculo") },
                onProfileClick = { navController.navigate("perfil") },
                onMarcarComoEntregue = { pedidoId ->
                    coroutineScope.launch {
                        db.pedidoDao().atualizarStatus(pedidoId, "ENTREGUE")
                    }
                }
            )
        }

        composable("cadastro_veiculo") {
            CadastroVeiculoScreen(
                onSalvar = { placa, modelo, marca, ano ->
                    val email = auth.currentUser?.email ?: ""

                    coroutineScope.launch {
                        val novoVeiculo = VeiculoEntity(
                            placa = placa,
                            emailDono = email,
                            modelo = modelo,
                            marca = marca,
                            ano = ano
                        )
                        db.veiculoDao().salvarVeiculo(novoVeiculo)
                    }
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        // --- TELA DE MENU DO PERFIL ---
        composable("perfil") {
            com.example.marketplace.ui.screens.profile.ProfileMenuScreen(
                tipoPerfil = viewModel.usuarioLogado?.tipoPerfil ?: "",
                onMeusDadosClick = { navController.navigate("meus_dados") },
                onMeusPedidosClick = { navController.navigate("meus_pedidos") },
                onMeusVeiculosClick = { navController.navigate("meus_veiculos") },
                onSignOutClick = {
                    auth.signOut()
                    viewModel.usuarioLogado = null
                    navController.navigate("login") { popUpTo(0) }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- TELA DE MEUS DADOS ---
        composable("meus_dados") {
            ProfileScreen(
                userEmail = auth.currentUser?.email ?: "Usuário",
                usuario = viewModel.usuarioLogado,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- TELA DE MEUS PEDIDOS (Somente Negociante) ---
        composable("meus_pedidos") {
            val email = auth.currentUser?.email ?: ""
            val meusPedidosList by db.pedidoDao().buscarPedidosDoCliente(email).collectAsState(initial = emptyList())

            val pedidosVisuais = meusPedidosList.map {
                com.example.marketplace.ui.screens.profile.PedidoCliente(
                    id = it.id,
                    enderecoEntrega = it.enderecoEntrega,
                    resumoItens = it.resumoItens,
                    valorTotal = it.valorTotal,
                    statusEntrega = it.statusEntrega
                )
            }

            com.example.marketplace.ui.screens.profile.MeusPedidosScreen(
                pedidos = pedidosVisuais,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- TELA DE MEUS VEÍCULOS (Somente Entregador) ---
        composable("meus_veiculos") {
            val email = auth.currentUser?.email ?: ""
            val veiculosList by db.veiculoDao().buscarVeiculosDoEntregador(email).collectAsState(initial = emptyList())

            val veiculosVisuais = veiculosList.map {
                com.example.marketplace.ui.screens.profile.VeiculoTemp(it.placa, it.modelo, it.marca, it.ano)
            }

            com.example.marketplace.ui.screens.profile.MeusVeiculosScreen(
                veiculos = veiculosVisuais,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// 3. O "Cofre": Aqui criamos o ViewModel que segura a memória do app!
class AppViewModel : ViewModel() {
    var usuarioLogado by mutableStateOf<UsuarioTemp?>(null)
    val carrinhoItens = mutableStateListOf<ItemCarrinho>()
}