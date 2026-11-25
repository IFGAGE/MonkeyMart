package com.example.geofencingpoc;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.location.Location;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.Collections;
import java.util.UUID;


final class MonitorGeofence {
    // Centro da entrega e raio em metros. Tela, pontos de teste e geofence usam estes valores.
    static final double LATITUDE = -21.78443;
    static final double LONGITUDE = -48.20406;
    static final float RAIO = 200f;
    // Categoria usada para publicar a notificação.
    static final String CANAL = "proximidade";

    // Estado local compartilhado com a tela e o receptor de eventos.
    static SharedPreferences estado(Context contexto) {
        return contexto.getSharedPreferences("geofence_poc", Context.MODE_PRIVATE);
    }

    // Rejeita um teste salvo com outro destino ou raio.
    static boolean configuracaoAtual(SharedPreferences estado) {
        return estado.getLong("destinoLatitude", Long.MIN_VALUE) == Double.doubleToLongBits(LATITUDE) &&
            estado.getLong("destinoLongitude", Long.MIN_VALUE) == Double.doubleToLongBits(LONGITUDE) &&
            estado.getFloat("raio", -1f) == RAIO;
    }

    // A janela de coordenadas chama aqui para obter pontos fora e dentro da área. Não altera o GPS.
    static Location pontoParaSimular(double distanciaMetros) {
        double latitude = Math.toRadians(LATITUDE);
        double longitude = Math.toRadians(LONGITUDE);
        double arco = distanciaMetros / 6_371_000.0;
        double latitudeDoPonto = Math.asin(Math.max(-1.0, Math.min(1.0,
            Math.sin(latitude) * Math.cos(arco) + Math.cos(latitude) * Math.sin(arco))));
        double longitudeDoPonto = longitude + Math.atan2(0.0,
            Math.cos(arco) - Math.sin(latitude) * Math.sin(latitudeDoPonto));
        Location ponto = new Location("ponto-de-teste");
        ponto.setLatitude(Math.toDegrees(latitudeDoPonto));
        ponto.setLongitude((Math.toDegrees(longitudeDoPonto) + 540.0) % 360.0 - 180.0);
        return ponto;
    }

    static void criarCanal(Context contexto) {
        if (Build.VERSION.SDK_INT >= 26) {
            contexto.getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CANAL, "Proximidade de entrega", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    // Instrução para o Android entregar os eventos ao GeofenceReceiver.onReceive().
    private static PendingIntent eventoPendente(Context contexto) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        Intent intent = new Intent(contexto, GeofenceReceiver.class);
        return PendingIntent.getBroadcast(contexto, 0, intent, flags);
    }

    // Consultado pela MainActivity antes de registrar: precisa e em segundo plano.
    static boolean temPermissoes(Context contexto) {
        return contexto.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 29 || contexto.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED);
    }

    @android.annotation.SuppressLint("MissingPermission")
    // MainActivity.executarRegistro() chama aqui: cadastra a área no serviço de geofencing.
    static Task<Void> iniciar(Context contexto) {
        if (!temPermissoes(contexto)) return Tasks.forException(
            new SecurityException("Permita a localização precisa e o acesso o tempo todo antes de iniciar o monitoramento."));
        SharedPreferences estado = estado(contexto);
        String id = UUID.randomUUID().toString();
        estado.edit().putString("id", id).putBoolean("ativo", true).putBoolean("registrado", false)
            .putLong("destinoLatitude", Double.doubleToLongBits(LATITUDE))
            .putLong("destinoLongitude", Double.doubleToLongBits(LONGITUDE)).putFloat("raio", RAIO)
            .putBoolean("avisado", false).putString("evento", "Iniciando teste…").apply();
        contexto.getSystemService(NotificationManager.class).cancel(1);

        // Define o círculo e os eventos ENTER/EXIT. A resposta em 5 segundos é apenas uma preferência.
        Geofence geofence = new Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(LATITUDE, LONGITUDE, RAIO)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(5_000)
            .build();
        GeofencingRequest pedido = new GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build();
        // Remove a área anterior e depois registra a nova com addGeofences().
        var cliente = LocationServices.getGeofencingClient(contexto);
        Task<Void> removerAnterior = cliente.removeGeofences(eventoPendente(contexto));
        return removerAnterior.continueWithTask(tarefa -> {
            if (!tarefa.isSuccessful()) throw tarefa.getException();
            if (!id.equals(estado.getString("id", "")) || !estado.getBoolean("ativo", false)) {
                return Tasks.forResult(null);
            }
            return cliente.addGeofences(pedido, eventoPendente(contexto));
        }).addOnSuccessListener(ignorado -> {
            if (!id.equals(estado.getString("id", "")) || !estado.getBoolean("ativo", false)) {
                cliente.removeGeofences(Collections.singletonList(id));
                return;
            }
            SharedPreferences.Editor atualizar = estado.edit().putBoolean("registrado", true);
            if ("Iniciando teste…".equals(estado.getString("evento", ""))) {
                atualizar.putString("evento", "Aguardando entrada na área. O aviso pode demorar alguns minutos.");
            }
            atualizar.apply();
        }).addOnFailureListener(erro -> {
            if (id.equals(estado.getString("id", ""))) {
                estado.edit().putBoolean("ativo", false).putBoolean("registrado", false)
                    .putString("evento", "Falha ao iniciar. Confira localização e permissões. Erro: " + erro.getMessage()).apply();
            }
        });
    }

    // MainActivity.pararMonitoramento() chama aqui: desativa o teste e remove a área.
    static Task<Void> parar(Context contexto) {
        SharedPreferences estado = estado(contexto);
        String id = estado.getString("id", "");
        estado.edit().putBoolean("ativo", false).putBoolean("registrado", false)
            .putString("evento", "Teste encerrado. Você pode iniciar outro.").apply();
        if (id.isEmpty()) return Tasks.forResult(null);
        return LocationServices.getGeofencingClient(contexto).removeGeofences(eventoPendente(contexto));
    }
}
