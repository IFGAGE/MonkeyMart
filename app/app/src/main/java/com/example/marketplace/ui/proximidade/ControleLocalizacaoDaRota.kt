package com.example.marketplace.ui.proximidade

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun ControleLocalizacaoDaRota() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember(context) { context.getSystemService(LocationManager::class.java) }
    // Permissões e GPS: conferidos na abertura e quando o Android avisa que a tela voltou.
    var localizacaoAtiva by remember { mutableStateOf(false) }
    var precisa by remember { mutableStateOf(false) }
    var segundoPlano by remember { mutableStateOf(false) }
    var autorizar by rememberSaveable { mutableStateOf(true) }
    var pediuPrecisa by rememberSaveable { mutableStateOf(false) }
    var pediuSegundoPlano by rememberSaveable { mutableStateOf(false) }
    var pediuNotificacoes by rememberSaveable { mutableStateOf(false) }
    var mostrarConfiguracoes by rememberSaveable { mutableStateOf(false) }
    val mensagem by MonitorGeofence.mensagem.collectAsState()
    fun atualizar() {
        localizacaoAtiva = manager != null && LocationManagerCompat.isLocationEnabled(manager)
        precisa = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        segundoPlano = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        MonitorGeofence.revisar()
    }
    // O Android entrega a resposta a estes retornos; não usamos os códigos 1 e 2 da PoC Java.
    val pedirLocalizacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado[Manifest.permission.ACCESS_FINE_LOCATION] != true) autorizar = false
        atualizar()
    }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (!concedida) autorizar = false
        atualizar()
    }
    val pedirNotificacoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { atualizar() }
    DisposableEffect(lifecycle, context) {
        atualizar()
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) atualizar()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Rota iniciada: solicita as permissões em sequência, sem repetir pedidos negados automaticamente.
    LaunchedEffect(autorizar, precisa, segundoPlano, localizacaoAtiva) {
        if (!autorizar || !localizacaoAtiva) return@LaunchedEffect
        when {
            !precisa && !pediuPrecisa -> {
                pediuPrecisa = true
                pedirLocalizacao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            precisa && !segundoPlano && !pediuSegundoPlano -> {
                pediuSegundoPlano = true
                if (Build.VERSION.SDK_INT == 29) pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else mostrarConfiguracoes = true
            }
            precisa && segundoPlano && Build.VERSION.SDK_INT >= 33 && !pediuNotificacoes -> {
                pediuNotificacoes = true
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    pedirNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    if (mostrarConfiguracoes) AlertDialog(
        onDismissRequest = { mostrarConfiguracoes = false; autorizar = false },
        title = { Text("Localização o tempo todo") },
        text = { Text("Em Permissões → Localização, escolha Permitir o tempo todo. Assim a aproximação funciona ao alternar de aplicativo.") },
        confirmButton = { TextButton(onClick = {
            mostrarConfiguracoes = false
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Abrir configurações") } },
        dismissButton = { TextButton(onClick = { mostrarConfiguracoes = false; autorizar = false }) { Text("Agora não") } }
    )
    Column {
        Text(if (precisa && segundoPlano && localizacaoAtiva) mensagem else "Ative a localização para avisar o comprador ao se aproximar.",
            style = MaterialTheme.typography.bodySmall)
        if (!localizacaoAtiva) TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }) { Text("Ativar localização") }
        else if (!precisa || !segundoPlano) TextButton(onClick = {
            // Configurações também permite recuperar uma permissão negada permanentemente.
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Permitir localização") }
        else TextButton(onClick = { MonitorGeofence.revisar() }) { Text("Atualizar monitoramento") }
    }
}
