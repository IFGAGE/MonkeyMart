package com.example.geofencingpoc;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofenceStatusCodes;


public final class GeofenceReceiver extends BroadcastReceiver {
    @Override
    // O Android chama aqui quando o serviço entrega um evento da área registrada.
    public void onReceive(Context contexto, Intent intent) {
        SharedPreferences estado = MonitorGeofence.estado(contexto);
        // Ignora teste encerrado ou configuração antiga.
        if (!estado.getBoolean("ativo", false) || !MonitorGeofence.configuracaoAtual(estado)) return;
        GeofencingEvent evento = GeofencingEvent.fromIntent(intent);
        if (evento == null) return;
        if (evento.hasError()) {
            estado.edit().putBoolean("ativo", false).putBoolean("registrado", false)
                .putString("evento", "Falha no teste: " +
                    GeofenceStatusCodes.getStatusCodeString(evento.getErrorCode()) + ". Confira localização e permissões e inicie outro teste.").apply();
            return;
        }
        if (evento.getTriggeringGeofences() == null) return;
        String id = estado.getString("id", "");
        boolean corresponde = false;
        for (Geofence geofence : evento.getTriggeringGeofences()) {
            if (id.equals(geofence.getRequestId())) corresponde = true;
        }
        if (!corresponde) return;
        // ENTER = entrou; EXIT = saiu. A primeira entrada chama notificar().
        int transicao = evento.getGeofenceTransition();
        if (transicao == Geofence.GEOFENCE_TRANSITION_EXIT) {
            estado.edit().putString("evento", "Saiu da área de entrega (EXIT).").apply();
        } else if (transicao == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Evita avisar duas vezes no mesmo teste.
            if (estado.getBoolean("avisado", false)) {
                estado.edit().putString("evento", "Entrou novamente (ENTER). Aviso já registrado.").apply();
                return;
            }
            estado.edit().putBoolean("avisado", true)
                .putString("evento", "Entregador próximo! Entrada detectada (ENTER).").apply();
            notificar(contexto, estado);
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    // Chamado por onReceive(): publica o aviso se as notificações estiverem permitidas.
    private void notificar(Context contexto, SharedPreferences estado) {
        NotificationManager notificacoes = contexto.getSystemService(NotificationManager.class);
        MonitorGeofence.criarCanal(contexto);
        if ((Build.VERSION.SDK_INT >= 33 && contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) ||
                (Build.VERSION.SDK_INT >= 24 && !notificacoes.areNotificationsEnabled())) {
            estado.edit().putString("evento",
                "Entregador próximo (ENTER). Ative as notificações nas configurações para receber o aviso.").apply();
            return;
        }
        // Tocar no aviso faz o Android abrir MainActivity.
        Intent abrir = new Intent(contexto, MainActivity.class);
        PendingIntent abrirApp = PendingIntent.getActivity(contexto, 1, abrir,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder aviso = Build.VERSION.SDK_INT >= 26 ?
            new Notification.Builder(contexto, MonitorGeofence.CANAL) : new Notification.Builder(contexto);
        notificacoes.notify(1, aviso.setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Entregador por perto")
            .setContentText("Seu entregador está perto do endereço de entrega.")
            .setContentIntent(abrirApp).setAutoCancel(true).build());
    }
}
