package com.example.mapapoc

import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.example.mapapoc.ui.theme.MapaPocTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "MonkeyMartEntrega5POC/1.0 (seu-email-real@gmail.com)"

        setContent {
            MapaPocTheme {
                var enderecoInput by remember { mutableStateOf("") }
                var pontoEntrega by remember { mutableStateOf<GeoPoint?>(null) }
                var carregando by remember { mutableStateOf(false) }
                var erroBusca by remember { mutableStateOf(false) }

                LaunchedEffect(carregando) {
                    if (carregando && enderecoInput.isNotBlank()) {
                        erroBusca = false
                        withContext(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(ctx, Locale.getDefault())
                                val list = geocoder.getFromLocationName(enderecoInput, 1)
                                if (!list.isNullOrEmpty()) {
                                    val addr = list[0]
                                    pontoEntrega = GeoPoint(addr.latitude, addr.longitude)
                                } else {
                                    erroBusca = true
                                }
                            } catch (e: Exception) {
                                erroBusca = true
                            }
                        }
                        carregando = false
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Validação: Geocoding + OSM") })
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = enderecoInput,
                                    onValueChange = { enderecoInput = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Digite o endereço") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { carregando = true },
                                    enabled = !carregando && enderecoInput.isNotBlank()
                                ) {
                                    Text("Buscar")
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (carregando) {
                                    CircularProgressIndicator()
                                } else if (pontoEntrega != null) {
                                    AndroidView(
                                        factory = { context ->
                                            MapView(context).apply {
                                                setTileSource(TileSourceFactory.MAPNIK)
                                                setMultiTouchControls(true)
                                                controller.setZoom(18.0)
                                            }
                                        },
                                        update = { view ->
                                            view.controller.setCenter(pontoEntrega)
                                            view.overlays.clear()
                                            val marcador = Marker(view).apply {
                                                position = pontoEntrega
                                                title = enderecoInput
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            }
                                            view.overlays.add(marcador)
                                            marcador.showInfoWindow()
                                            view.invalidate()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (erroBusca) {
                                    Text(
                                        text = "Endereço não encontrado.",
                                        color = Color.Red,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Insira um endereço para visualizar no mapa.",
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}