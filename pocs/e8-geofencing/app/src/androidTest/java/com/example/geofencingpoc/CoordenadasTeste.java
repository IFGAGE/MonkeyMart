package com.example.geofencingpoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class CoordenadasTeste {
    @Test public void pontosDeSimulacaoUsamODestinoAtualEFicamForaEDentroDoRaio() {
        Location fora = MonitorGeofence.pontoParaSimular(MonitorGeofence.RAIO * 2.0);
        Location dentro = MonitorGeofence.pontoParaSimular(MonitorGeofence.RAIO / 2.0);
        float[] distancia = new float[1];
        Location.distanceBetween(MonitorGeofence.LATITUDE, MonitorGeofence.LONGITUDE,
            fora.getLatitude(), fora.getLongitude(), distancia);
        assertEquals(MonitorGeofence.RAIO * 2.0, distancia[0], 3.0);
        assertTrue(distancia[0] > MonitorGeofence.RAIO);
        Location.distanceBetween(MonitorGeofence.LATITUDE, MonitorGeofence.LONGITUDE,
            dentro.getLatitude(), dentro.getLongitude(), distancia);
        assertEquals(MonitorGeofence.RAIO / 2.0, distancia[0], 3.0);
        assertTrue(distancia[0] < MonitorGeofence.RAIO);
    }

    @Test public void destinoOuRaioAlteradoInvalidaORegistroAnterior() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences estado = contexto.getSharedPreferences("verificacao_coordenadas", Context.MODE_PRIVATE);
        try {
            estado.edit().clear().commit();
            assertFalse(MonitorGeofence.configuracaoAtual(estado));
            estado.edit().putLong("destinoLatitude", Double.doubleToLongBits(MonitorGeofence.LATITUDE))
                .putLong("destinoLongitude", Double.doubleToLongBits(MonitorGeofence.LONGITUDE))
                .putFloat("raio", MonitorGeofence.RAIO).commit();
            assertTrue(MonitorGeofence.configuracaoAtual(estado));
            estado.edit().putLong("destinoLatitude", Double.doubleToLongBits(MonitorGeofence.LATITUDE + 0.01)).commit();
            assertFalse(MonitorGeofence.configuracaoAtual(estado));
            estado.edit().putLong("destinoLatitude", Double.doubleToLongBits(MonitorGeofence.LATITUDE))
                .putFloat("raio", MonitorGeofence.RAIO + 10f).commit();
            assertFalse(MonitorGeofence.configuracaoAtual(estado));
        } finally {
            estado.edit().clear().commit();
        }
    }
}
