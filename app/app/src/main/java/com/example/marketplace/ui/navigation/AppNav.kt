package com.example.marketplace.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
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
import androidx.navigation.navArgument
import com.example.marketplace.ui.mapa.Coordenadas
import com.example.marketplace.ui.mapa.TipoParada
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.marketplace.ui.mapa.RotaGerada

import com.example.marketplace.data.AppDatabase
import com.example.marketplace.data.entity.AvaliacaoEntity
import com.example.marketplace.data.entity.PedidoEntity
import com.example.marketplace.data.entity.ProdutoEntity
import com.example.marketplace.data.entity.UsuarioEntity
import com.example.marketplace.data.entity.VeiculoEntity
import com.example.marketplace.data.sync.acompanharPedidos
import com.example.marketplace.data.sync.acompanharProdutos
import com.example.marketplace.data.sync.iniciarRotaComReserva
import com.example.marketplace.data.sync.cancelarRotaDoEntregador
import com.example.marketplace.ui.screens.negociante.PedidosClientesScreen
import com.example.marketplace.data.sync.sincronizarAvaliacoes
import com.example.marketplace.data.sync.sincronizarPedidos
import com.example.marketplace.data.sync.sincronizarProdutos
import com.example.marketplace.data.sync.sincronizarTudo
import com.example.marketplace.data.sync.sincronizarUsuarios
import com.example.marketplace.data.sync.sincronizarVeiculos

import com.example.marketplace.ui.screens.auth.LoginScreen
import com.example.marketplace.ui.screens.profile.ProfileSelectionScreen
import com.example.marketplace.ui.screens.negociante.NegocianteHomeScreen
import com.example.marketplace.ui.screens.negociante.CadastroProdutoScreen
import com.example.marketplace.ui.screens.negociante.AreaNegocianteScreen
import com.example.marketplace.ui.screens.negociante.EnderecoColetaScreen
import com.example.marketplace.ui.screens.negociante.MeusProdutosScreen
import com.example.marketplace.ui.screens.negociante.ProdutoTemp
import com.example.marketplace.ui.screens.negociante.ItemCarrinho
import com.example.marketplace.ui.screens.negociante.CarrinhoScreen
import com.example.marketplace.ui.screens.entregador.EntregadorHomeScreen
import com.example.marketplace.ui.screens.entregador.CadastroVeiculoScreen
import com.example.marketplace.ui.screens.entregador.MapaEntregaScreen
import com.example.marketplace.ui.screens.entregador.RotaEntregaScreen
import com.example.marketplace.ui.mapa.gerarRota
import com.example.marketplace.ui.mapa.paraEntity
import com.example.marketplace.ui.mapa.paraRotaGerada
import com.example.marketplace.ui.mapa.pedidoIds
import com.example.marketplace.ui.screens.profile.ProfileScreen
import com.example.marketplace.ui.screens.profile.UsuarioTemp

@Composable
fun AppNavigation(
    viewModel: AppViewModel = viewModel()
) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    var usuarioId by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { usuarioId = it.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    LaunchedEffect(usuarioId) {
        if (usuarioId != null) {
            launch { acompanharPedidos(db.pedidoDao()) }
            launch { acompanharProdutos(db.produtoDao()) }
        }
    }

    val startDestination = if (auth.currentUser != null) "check_profile" else "login"

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

                        try {
                            sincronizarUsuarios(db.usuarioDao())
                        } catch (e: Exception) {
                            Log.e("Sync", "Offline: será reenviado automaticamente depois.", e)
                        }

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

        composable("area_negociante") {
            val email = auth.currentUser?.email ?: ""
            val produtosList by db.produtoDao().buscarTodosProdutos().collectAsState(initial = emptyList())

            LaunchedEffect(Unit) {
                sincronizarTudo(db)
            }

            val pedidosEntregues by db.pedidoDao().buscarPedidosEntregues(email).collectAsState(initial = emptyList())
            val historicoComprasEntregues = pedidosEntregues.joinToString { it.resumoItens }

            val avaliacoesGerais by db.avaliacaoDAO().buscarTodas().collectAsState(initial = emptyList())

            val produtosVisuais = produtosList.filter { it.podeSerCompradoPor(email) }.map { produtoEntity ->
                val avaliacoesDoProduto = avaliacoesGerais
                    .filter { it.nomeProduto == produtoEntity.nome }
                    .map { com.example.marketplace.ui.screens.negociante.AvaliacaoTemp(it.nota, it.comentario, it.emailAutor) }

                val jaAvaliou = avaliacoesDoProduto.any { it.emailAutor == email }

                ProdutoTemp(
                    nome = produtoEntity.nome,
                    preco = produtoEntity.preco,
                    descricao = produtoEntity.descricao,
                    nomeDono = produtoEntity.nomeNegociante,
                    fotoPathLocal = produtoEntity.fotoPathLocal,
                    avaliacoes = avaliacoesDoProduto,
                    podeAvaliar = historicoComprasEntregues.contains(produtoEntity.nome) && !jaAvaliou,
                    id = produtoEntity.id,
                    emailDono = produtoEntity.emailNegociante
                )
            }

            NegocianteHomeScreen(
                produtos = produtosVisuais,
                quantidadeCarrinho = carrinhoItens.sumOf { it.quantidade },
                onAddProdutoClick = { navController.navigate("painel_negociante") },
                onProfileClick = { navController.navigate("perfil") },
                onCartClick = { navController.navigate("carrinho") },
                onAddToCart = adicionar@{ produtoSelecionado ->
                    if (email.isBlank() || produtoSelecionado.emailDono == email) return@adicionar
                    val itemExistente = carrinhoItens.find { it.produto.id == produtoSelecionado.id }
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
                        try {
                            sincronizarAvaliacoes(db.avaliacaoDAO())
                        } catch (e: Exception) {
                            Log.e("Sync", "Offline: será reenviada automaticamente depois.", e)
                        }
                    }
                }
            )
        }

        composable("painel_negociante") {
            val email = auth.currentUser?.email.orEmpty()
            val usuario by db.usuarioDao().observarUsuario(email).collectAsState(initial = null)
            val usuarioAtual = usuario
            val temEndereco = usuarioAtual?.enderecoColeta?.valido() == true
            LaunchedEffect(usuarioAtual) {
                if (usuarioAtual != null && !temEndereco) {
                    navController.navigate("endereco_coleta") {
                        popUpTo("painel_negociante") { inclusive = true }
                    }
                }
            }
            if (temEndereco) {
                AreaNegocianteScreen(
                    onEndereco = { navController.navigate("endereco_coleta") },
                    onNovoProduto = { navController.navigate("cadastro_produto") },
                    onMeusProdutos = { navController.navigate("meus_produtos") },
                    onPedidosClientes = { navController.navigate("pedidos_clientes") },
                    onVoltar = { navController.popBackStack() }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }

        composable("endereco_coleta") {
            val email = auth.currentUser?.email.orEmpty()
            val usuario by db.usuarioDao().observarUsuario(email).collectAsState(initial = null)
            val usuarioAtual = usuario
            if (usuarioAtual == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                EnderecoColetaScreen(
                    enderecoInicial = usuarioAtual.enderecoColeta,
                    onSalvar = { endereco ->
                        check(endereco.valido())
                        val usuarioSalvo = checkNotNull(db.usuarioDao().buscarUsuario(email))
                        db.usuarioDao().salvarColeta(usuarioSalvo.copy(enderecoColeta = endereco, isSynced = false))
                        // A navegação depende do salvamento local, sem aguardar a rede.
                        coroutineScope.launch {
                            try { sincronizarUsuarios(db.usuarioDao()) }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (e: Exception) { Log.e("Sync", "Endereço local pendente de sincronização.", e) }
                        }
                        navController.navigate("painel_negociante") {
                            popUpTo("endereco_coleta") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onVoltar = { navController.popBackStack() }
                )
            }
        }

        composable("meus_produtos") {
            val email = auth.currentUser?.email.orEmpty()
            val produtos by db.produtoDao().buscarProdutosDoNegociante(email).collectAsState(initial = emptyList())
            MeusProdutosScreen(produtos = produtos, onVoltar = { navController.popBackStack() })
        }

        composable("pedidos_clientes") {
            val email = auth.currentUser?.email.orEmpty()
            val pedidos by db.pedidoDao().buscarPedidosDoVendedor(email).collectAsState(initial = emptyList())
            PedidosClientesScreen(pedidos, onPronto = { id ->
                check(db.pedidoDao().marcarPronto(id, email) == 1) { "O pedido já foi atualizado." }
                coroutineScope.launch {
                    try { sincronizarPedidos(db.pedidoDao()) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (e: Exception) { Log.e("Sync", "Preparo aguardando sincronização.", e) }
                }
            }, onVoltar = { navController.popBackStack() })
        }

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
                onFinalizarPedido = { endereco, _ ->
                    val email = checkNotNull(auth.currentUser?.email)
                    db.pedidoDao().finalizarCompra(email, carrinhoItens.associate { it.produto.id to it.quantidade }, endereco)
                    carrinhoItens.clear()
                    // A confirmação depende só do SQLite, mesmo quando o aparelho está offline.
                    coroutineScope.launch {
                        try {
                            sincronizarPedidos(db.pedidoDao())
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("Sync", "Pedido local pendente de sincronização.", e)
                        }
                    }
                }
            )
        }

        composable("cadastro_produto") {
            val email = auth.currentUser?.email.orEmpty()
            val usuario by db.usuarioDao().observarUsuario(email).collectAsState(initial = null)
            val usuarioAtual = usuario
            val temEndereco = usuarioAtual?.enderecoColeta?.valido() == true
            LaunchedEffect(usuarioAtual) {
                if (usuarioAtual != null && !temEndereco) {
                    navController.navigate("endereco_coleta") {
                        popUpTo("cadastro_produto") { inclusive = true }
                    }
                }
            }
            if (temEndereco) CadastroProdutoScreen(
                onSalvar = { nome, descricao, preco, fotoPathLocal ->
                    val email = auth.currentUser?.email ?: ""
                    val nomeNegociante = viewModel.usuarioLogado?.nome ?: "Desconhecido"

                    coroutineScope.launch {
                        val coleta = checkNotNull(db.usuarioDao().buscarUsuario(email)?.enderecoColeta?.validado())
                        val novoProduto = ProdutoEntity(
                            emailNegociante = email,
                            nomeNegociante = nomeNegociante,
                            nome = nome,
                            descricao = descricao,
                            preco = preco,
                            fotoPathLocal = fotoPathLocal,
                            isSynced = false,
                            enderecoColeta = coleta.endereco,
                            coletaLatitude = coleta.latitude,
                            coletaLongitude = coleta.longitude
                        )
                        db.produtoDao().salvarProduto(novoProduto)

                        try {
                            sincronizarProdutos(db.produtoDao())
                        } catch (e: Exception) {
                            Log.e("Sync", "Offline: será reenviado automaticamente depois.", e)
                        }
                    }
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        composable("area_entregador") {
            val email = auth.currentUser?.email ?: ""
            var gerandoRota by remember { mutableStateOf(false) }
            var erroRota by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                sincronizarTudo(db)
            }

            val pedidosPendentes by db.pedidoDao().buscarPedidosPendentes().collectAsState(initial = emptyList())
            val veiculosCadastrados by db.veiculoDao().buscarVeiculosDoEntregador(email).collectAsState(initial = emptyList())
            val rotaAtual by db.rotaDao().observar(email).collectAsState(initial = null)
            val idsAtuais = remember(rotaAtual) { rotaAtual?.let { runCatching { it.pedidoIds() }.getOrDefault(emptyList()) }.orEmpty() }
            val pedidosDaRota by db.pedidoDao().buscarPorIds(idsAtuais).collectAsState(initial = emptyList())
            val podeCancelarRota = rotaAtual?.estado != "INICIADA" ||
                (pedidosDaRota.size == idsAtuais.size && pedidosDaRota.none { it.coletado || it.statusEntrega == "ENTREGUE" })
            val pedidosVisuais = pedidosPendentes.map {
                com.example.marketplace.ui.screens.entregador.PedidoTemp(
                    id = it.id,
                    emailCliente = it.emailCliente,
                    enderecoEntrega = it.enderecoEntrega,
                    resumoItens = it.resumoItens,
                    valorTotal = it.valorTotal,
                    statusEntrega = it.statusEntrega,
                    enderecoColeta = it.enderecoColeta,
                    coletaLatitude = it.coletaLatitude, coletaLongitude = it.coletaLongitude,
                    entregaLatitude = it.entregaLatitude, entregaLongitude = it.entregaLongitude, coletado = it.coletado
                )
            }

            EntregadorHomeScreen(
                pedidos = pedidosVisuais,
                rota = rotaAtual,
                podeCancelarRota = podeCancelarRota,
                temVeiculo = veiculosCadastrados.isNotEmpty(),
                gerandoRota = gerandoRota,
                erroRota = erroRota,
                onAddVeiculoClick = { navController.navigate("cadastro_veiculo") },
                onProfileClick = { navController.navigate("perfil") },
                onGerarRota = { selecionados ->
                    coroutineScope.launch {
                        gerandoRota = true
                        erroRota = null
                        try {
                            check(veiculosCadastrados.isNotEmpty()) { "Cadastre um veículo para gerar uma rota." }
                            val rota = gerarRota(context.applicationContext, selecionados)
                            db.rotaDao().salvar(rota.paraEntity(email))
                            navController.navigate("rota_entrega")
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            erroRota = e.message ?: "Não foi possível gerar a rota."
                        } finally {
                            gerandoRota = false
                        }
                    }
                },
                onContinuarRota = { navController.navigate("rota_entrega") },
                onCancelarRota = { esperada ->
                    coroutineScope.launch {
                        gerandoRota = true
                        erroRota = null
                        try {
                            cancelarRotaDoEntregador(db, esperada)
                            coroutineScope.launch {
                                try { sincronizarPedidos(db.pedidoDao()) }
                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (e: Exception) { Log.e("Sync", "Cancelamento pendente de sincronização.", e) }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { erroRota = e.message ?: "Não foi possível cancelar a rota." }
                        finally { gerandoRota = false }
                    }
                },
                onPermissaoNegada = { erroRota = "Permita o acesso à localização para gerar a rota." },
                onVerMapaClick = { local ->
                    val latitude = local.coordenadas?.latitude?.toString().orEmpty()
                    val longitude = local.coordenadas?.longitude?.toString().orEmpty()
                    navController.navigate("mapa_entrega/${local.pedidoId}?endereco=${Uri.encode(local.endereco)}" +
                        "&tipo=${local.tipo.name}&latitude=$latitude&longitude=$longitude")
                }
            )
        }

        composable("rota_entrega") {
            val email = auth.currentUser?.email ?: ""
            val rota by db.rotaDao().observar(email).collectAsState(initial = null)
            val rotaAtual = rota

            if (rotaAtual == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Trocar veículo ou estado não altera a geometria. Ler um trajeto
                // grande não deve bloquear a thread que desenha e recebe toques.
                val dadosRota by key(rotaAtual.pedidosJson, rotaAtual.origemJson, rotaAtual.destinosJson, rotaAtual.geometriaJson) {
                    produceState<Result<RotaGerada>?>(initialValue = null) {
                        value = withContext(Dispatchers.Default) {
                            val inicio = android.os.SystemClock.elapsedRealtime()
                            runCatching { rotaAtual.paraRotaGerada() }.also {
                                Log.d("RotaDesempenho",
                                    "Trajeto salvo lido em ${android.os.SystemClock.elapsedRealtime() - inicio}ms")
                            }
                        }
                    }
                }
                val pedidoIds = remember(rotaAtual.pedidosJson) { runCatching { rotaAtual.pedidoIds() }.getOrDefault(emptyList()) }
                val pedidos by db.pedidoDao().buscarPorIds(pedidoIds).collectAsState(initial = emptyList())
                val veiculos by db.veiculoDao().buscarVeiculosDoEntregador(email).collectAsState(initial = emptyList())
                var processando by remember { mutableStateOf(false) }
                var erro by remember { mutableStateOf<String?>(null) }

                val pedidosOrdenados = pedidoIds.mapNotNull { pedidoId ->
                    pedidos.find { it.id == pedidoId }?.let {
                        com.example.marketplace.ui.screens.entregador.PedidoTemp(
                            id = it.id,
                            emailCliente = it.emailCliente,
                            enderecoEntrega = it.enderecoEntrega,
                            resumoItens = it.resumoItens,
                            valorTotal = it.valorTotal,
                            statusEntrega = it.statusEntrega,
                            enderecoColeta = it.enderecoColeta,
                            coletaLatitude = it.coletaLatitude, coletaLongitude = it.coletaLongitude,
                            entregaLatitude = it.entregaLatitude, entregaLongitude = it.entregaLongitude, coletado = it.coletado
                        )
                    }
                }
                val veiculosVisuais = veiculos.map {
                    com.example.marketplace.ui.screens.profile.VeiculoTemp(it.placa, it.modelo, it.marca, it.ano)
                }

                if (dadosRota == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                dadosRota?.fold(
                    onSuccess = { rotaGerada ->
                        RotaEntregaScreen(
                            rota = rotaAtual,
                            rotaGerada = rotaGerada,
                            pedidos = pedidosOrdenados,
                            veiculos = veiculosVisuais,
                            processando = processando,
                            erro = erro,
                            onVoltar = { navController.popBackStack() },
                            onCadastrarVeiculo = { navController.navigate("cadastro_veiculo") },
                            onRecalcular = {
                                coroutineScope.launch {
                                    processando = true
                                    erro = null
                                    try {
                                        check(pedidosOrdenados.size == pedidoIds.size) { "Aguarde o carregamento dos pedidos." }
                                        val novaRota = gerarRota(context.applicationContext, pedidosOrdenados).paraEntity(email)
                                        check(db.rotaDao().atualizarPlanejada(email, novaRota.pedidosJson, novaRota.origemJson,
                                            novaRota.destinosJson, novaRota.geometriaJson, rotaAtual.pedidosJson,
                                            rotaAtual.origemJson, rotaAtual.destinosJson, rotaAtual.geometriaJson) == 1) {
                                            "A rota mudou durante o cálculo. Abra-a novamente."
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                    catch (e: Exception) { erro = e.message ?: "Não foi possível recalcular a rota." }
                                    finally { processando = false }
                                }
                            },
                            onExcluir = {
                                coroutineScope.launch {
                                    processando = true
                                    try {
                                        cancelarRotaDoEntregador(db, rotaAtual)
                                        navController.popBackStack()
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        erro = e.message
                                    } finally {
                                        processando = false
                                    }
                                }
                            },
                            onIniciar = { placa ->
                                coroutineScope.launch {
                                    processando = true
                                    erro = null
                                    try {
                                        iniciarRotaComReserva(db, email, placa, pedidoIds)
                                        try {
                                            sincronizarPedidos(db.pedidoDao())
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.e("Sync", "Início da rota pendente de sincronização.", e)
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        erro = e.message ?: "Não foi possível iniciar a rota."
                                    } finally {
                                        processando = false
                                    }
                                }
                            },
                            onSelecionarVeiculo = { placa -> db.rotaDao().selecionarVeiculo(rotaAtual, placa) },
                            onAtualizarParada = { ids, tipo, concluido ->
                                coroutineScope.launch {
                                    processando = true
                                    erro = null
                                    try {
                                        if (tipo == com.example.marketplace.ui.mapa.TipoParada.COLETA)
                                            db.pedidoDao().atualizarColetas(ids, email, concluido)
                                        else db.pedidoDao().atualizarEntregas(ids, email, concluido)
                                        coroutineScope.launch {
                                            try { sincronizarPedidos(db.pedidoDao()) }
                                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                            catch (e: Exception) { Log.e("Sync", "Status local pendente de sincronização.", e) }
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        erro = e.message ?: "Não foi possível atualizar o pedido."
                                    } finally {
                                        processando = false
                                    }
                                }
                            },
                            onCancelar = {
                                coroutineScope.launch {
                                    processando = true
                                    try {
                                        cancelarRotaDoEntregador(db, rotaAtual)
                                        try {
                                            sincronizarPedidos(db.pedidoDao())
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.e("Sync", "Cancelamento local pendente de sincronização.", e)
                                        }
                                        navController.popBackStack()
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        erro = e.message ?: "Não foi possível cancelar a rota."
                                    } finally {
                                        processando = false
                                    }
                                }
                            },
                            onFinalizar = {
                                coroutineScope.launch {
                                    processando = true
                                    try {
                                        db.rotaDao().finalizar(email, pedidoIds)
                                        navController.popBackStack()
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        erro = e.message
                                    } finally {
                                        processando = false
                                    }
                                }
                            }
                        )
                    },
                    onFailure = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Text("Os dados salvos da rota são inválidos.")
                        }
                    }
                )
            }
        }

        composable("mapa_entrega/{pedidoId}?endereco={endereco}&tipo={tipo}&latitude={latitude}&longitude={longitude}",
            arguments = listOf(
                navArgument("tipo") { defaultValue = "ENTREGA" },
                navArgument("latitude") { defaultValue = "" },
                navArgument("longitude") { defaultValue = "" }
            )) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getString("pedidoId")?.toIntOrNull() ?: 0
            val endereco = backStackEntry.arguments?.getString("endereco") ?: "Endereço não informado"
            val tipo = if (backStackEntry.arguments?.getString("tipo") == "COLETA") TipoParada.COLETA else TipoParada.ENTREGA
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull()
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull()

            MapaEntregaScreen(
                pedidoId = pedidoId,
                endereco = endereco,
                tipo = tipo,
                coordenadas = if (latitude != null && longitude != null) Coordenadas(latitude, longitude) else null,
                onVoltar = { navController.popBackStack() }
            )
        }

        composable("cadastro_veiculo") {
            CadastroVeiculoScreen(
                onSalvar = { placa, modelo, marca, ano ->
                    val email = auth.currentUser?.email ?: ""

                    coroutineScope.launch {
                        val novoVeiculo = VeiculoEntity(
                            placa = placa,
                            emailEntregador = email,
                            modelo = modelo,
                            marca = marca,
                            ano = ano
                        )
                        db.veiculoDao().salvarVeiculo(novoVeiculo)
                        try {
                            sincronizarVeiculos(db.veiculoDao())
                        } catch (e: Exception) {
                            Log.e("Sync", "Offline: será reenviado automaticamente depois.", e)
                        }
                    }
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable("perfil") {
            com.example.marketplace.ui.screens.profile.ProfileMenuScreen(
                tipoPerfil = viewModel.usuarioLogado?.tipoPerfil ?: "",
                onMeusDadosClick = { navController.navigate("meus_dados") },
                onMeusPedidosClick = { navController.navigate("meus_pedidos") },
                onMeusVeiculosClick = { navController.navigate("meus_veiculos") },
                onSignOutClick = {
                    auth.signOut()
                    viewModel.usuarioLogado = null
                    carrinhoItens.clear()
                    navController.navigate("login") { popUpTo(0) }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("meus_dados") {
            ProfileScreen(
                userEmail = auth.currentUser?.email ?: "Usuário",
                usuario = viewModel.usuarioLogado,
                onBackClick = { navController.popBackStack() }
            )
        }

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

class AppViewModel : ViewModel() {
    var usuarioLogado by mutableStateOf<UsuarioTemp?>(null)
    val carrinhoItens = mutableStateListOf<ItemCarrinho>()
}
