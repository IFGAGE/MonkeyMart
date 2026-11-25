package com.example.marketplace.ui.proximidade

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

// Mesma fonte da PoC: recebe posições reais ou fornecidas pelo Mock Locations.
@SuppressLint("MissingPermission")
internal fun observarLocalizacao(context: Context) = callbackFlow<Location> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        close(); return@callbackFlow
    }
    val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val gps = if (debug) context.getSystemService(LocationManager::class.java) else null
    var ultimaSimuladaNs: Long? = null
    fun publicar(posicao: Location) {
        if (debug) {
            val agora = SystemClock.elapsedRealtimeNanos()
            val simulada = LocationCompat.isMock(posicao)
            if (simulada && agora - posicao.elapsedRealtimeNanos in 0L..5_000_000_000L &&
                posicao.hasAccuracy() && posicao.accuracy in 0f..100f) {
                val anterior = ultimaSimuladaNs
                if (anterior != null && posicao.elapsedRealtimeNanos <= anterior) return
                ultimaSimuladaNs = posicao.elapsedRealtimeNanos
            } else {
                if (simulada || ultimaSimuladaNs?.let { agora - it in 0L..5_000_000_000L } == true) return
            }
        }
        trySend(posicao)
    }

    val cliente = LocationServices.getFusedLocationProviderClient(context)
    val retorno = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            resultado.lastLocation?.let(::publicar)
        }
    }
    val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000)
        .setMinUpdateIntervalMillis(1_000).setMaxUpdateAgeMillis(0).build()
    cliente.requestLocationUpdates(pedido, retorno, Looper.getMainLooper()).addOnFailureListener { close(it) }
    // Em debug, também aceita posições simuladas fornecidas diretamente ao GPS do Android.
    val gpsSimulado = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            if (LocationCompat.isMock(location)) publicar(location)
        }
    }
    val inscrito = gps != null && runCatching {
        gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, gpsSimulado, Looper.getMainLooper())
    }.isSuccess
    awaitClose {
        cliente.removeLocationUpdates(retorno)
        if (inscrito) gps?.removeUpdates(gpsSimulado)
    }
}.conflate()
