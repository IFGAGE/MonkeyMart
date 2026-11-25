package com.example.geofencingpoc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import java.util.Locale;


public final class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    // Códigos dos pedidos de permissão: identificados em onRequestPermissionsResult().
    private static final int LOCALIZACAO = 1;
    private static final int SEGUNDO_PLANO = 2;

    // Serviço de posições e estado salvo: usados para acompanhar a distância e atualizar a tela.
    private FusedLocationProviderClient localizacao;
    private SharedPreferences preferencias;

    // Textos e botões da tela. ultimaPosicao guarda o último ponto para a janela de coordenadas.
    private TextView monitoramento;
    private TextView posicao;
    private Location ultimaPosicao;
    private TextView ultimoEvento;
    private Button registrar;
    private Button parar;

    // Busca de endereço: consultaEndereco pesquisa; telaPrincipal coloca o resultado na interface.
    private final ExecutorService consultaEndereco = Executors.newSingleThreadExecutor();
    private final Handler telaPrincipal = new Handler(Looper.getMainLooper());

    // operando: tarefa em andamento; inicioPendente: início aguardando permissão;
    // aguardandoConfiguracoes: conferir a autorização quando o usuário voltar.
    private boolean operando;
    private boolean inicioPendente;
    private boolean aguardandoConfiguracoes;

    // O Android entrega a posição aqui após observarPosicao(); seguimos para mostrarPosicao().
    private final LocationCallback retornoLocalizacao = new LocationCallback() {
        @Override public void onLocationResult(LocationResult resultado) {
            Location ponto = resultado.getLastLocation();
            if (ponto != null) mostrarPosicao(ponto);
        }
    };

    @Override
    protected void onCreate(Bundle salvo) {
        super.onCreate(salvo);
        if (salvo != null) {
            inicioPendente = salvo.getBoolean("inicioPendente");
            aguardandoConfiguracoes = salvo.getBoolean("aguardandoConfiguracoes");
        }
        preferencias = MonitorGeofence.estado(this);
        localizacao = LocationServices.getFusedLocationProviderClient(this);
        MonitorGeofence.criarCanal(this);
        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        int margem = Math.round(20 * getResources().getDisplayMetrics().density);
        tela.setPadding(margem, margem, margem, margem);
        tela.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                var barras = insets.getInsets(WindowInsets.Type.systemBars());
                tela.setPadding(margem + barras.left, margem + barras.top,
                    margem + barras.right, margem + barras.bottom);
            }
            return insets;
        });
        texto(tela, "Aviso de aproximação", 24);
        TextView destino = texto(tela, "Buscando o endereço da entrega…", 17);
        destino.setTextIsSelectable(true);
        buscarEndereco(destino);
        texto(tela, String.format(Locale.forLanguageTag("pt-BR"),
            "Avisar quando o entregador entrar na área de %.0f m ao redor da entrega.", MonitorGeofence.RAIO), 16);
        monitoramento = texto(tela, "", 17);
        posicao = texto(tela, "Aguardando localização…", 22);
        ultimoEvento = texto(tela, "", 17);
        registrar = botao(tela, "Iniciar teste", this::registrarPerimetro);
        parar = botao(tela, "Parar teste", this::pararMonitoramento);
        botao(tela, "Ver coordenadas para simular", this::mostrarCoordenadas);
        ScrollView rolagem = new ScrollView(this);
        rolagem.addView(tela);
        setContentView(rolagem);
        atualizarEstado();
        if (preferencias.getBoolean("ativo", false) && !MonitorGeofence.configuracaoAtual(preferencias)) {
            operando = true;
            atualizarEstado();
            MonitorGeofence.parar(getApplicationContext()).addOnCompleteListener(tarefa -> {
                operando = false;
                preferencias.edit().putString("evento", "Destino ou raio alterado. Inicie um novo teste.").apply();
                atualizarEstado();
            });
        }
    }

    private void registrarPerimetro() {
        inicioPendente = true;
        continuarInicio();
    }

    private void continuarInicio() {
        atualizarEstado();
        if (!MonitorGeofence.temPermissoes(this)) {
            pedirPermissoes();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3);
            return;
        }
        executarRegistro();
    }

    private void pedirPermissoes() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, LOCALIZACAO);
        } else if (!MonitorGeofence.temPermissoes(this)) {
            if (Build.VERSION.SDK_INT == 29) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, SEGUNDO_PLANO);
            } else {
                new AlertDialog.Builder(this).setTitle("Permitir localização o tempo todo")
                    .setMessage("Abra Permissões → Localização → Permitir o tempo todo. Depois volte para continuar o teste.")
                    .setPositiveButton("Abrir configurações", (dialogo, botao) -> {
                        aguardandoConfiguracoes = true;
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                    })
                    .setNegativeButton("Cancelar", (dialogo, botao) -> cancelarInicio())
                    .setOnCancelListener(dialogo -> cancelarInicio()).show();
            }
        } else {
            ultimoEvento.setText("Localização autorizada. Toque em Iniciar teste.");
        }
    }

    @Override public void onRequestPermissionsResult(int pedido, String[] permissoes, int[] resultados) {
        super.onRequestPermissionsResult(pedido, permissoes, resultados);
        if (pedido == 3) {
            if (inicioPendente) executarRegistro();
            return;
        }
        if (pedido != LOCALIZACAO && pedido != SEGUNDO_PLANO) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            cancelarInicio();
            ultimoEvento.setText("Ative a localização precisa e toque em Iniciar teste.");
            return;
        }
        observarPosicao();
        if (pedido == SEGUNDO_PLANO && !MonitorGeofence.temPermissoes(this)) {
            cancelarInicio();
            ultimoEvento.setText("Permita localização o tempo todo para iniciar.");
            return;
        }
        if (inicioPendente) continuarInicio();
        else if (!MonitorGeofence.temPermissoes(this)) pedirPermissoes();
        else ultimoEvento.setText("Localização autorizada. Toque em Iniciar teste.");
    }

    @Override protected void onResume() {
        super.onResume();
        preferencias.registerOnSharedPreferenceChangeListener(this);
        atualizarEstado();
        observarPosicao();
        if (aguardandoConfiguracoes) {
            aguardandoConfiguracoes = false;
            if (MonitorGeofence.temPermissoes(this)) {
                if (inicioPendente) continuarInicio();
            } else {
                cancelarInicio();
                ultimoEvento.setText("Permita localização o tempo todo e toque em Iniciar teste.");
            }
        }
    }

    private void executarRegistro() {
        inicioPendente = false;
        operando = true;
        atualizarEstado();
        MonitorGeofence.iniciar(getApplicationContext()).addOnCompleteListener(tarefa -> {
            operando = false;
            atualizarEstado();
        });
    }

    @SuppressLint("MissingPermission")
    private void observarPosicao() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest pedido = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L).setMaxUpdateAgeMillis(0L).build();
        localizacao.requestLocationUpdates(pedido, retornoLocalizacao, Looper.getMainLooper())
            .addOnFailureListener(erro -> posicao.setText("Sem localização. Confira o GPS, a permissão e o Google Play Services."));
    }

    // Mostra a distância recebida. A detecção de entrada e a notificação ficam no GeofenceReceiver.
    @SuppressWarnings("deprecation")
    private void mostrarPosicao(Location ponto) {
        long idade = (SystemClock.elapsedRealtimeNanos() - ponto.getElapsedRealtimeNanos()) / 1_000_000L;
        if (!ponto.hasAccuracy() || idade < 0 || idade > 30_000L) {
            posicao.setText("Aguardando uma posição atualizada…");
            return;
        }
        ultimaPosicao = new Location(ponto);
        float[] distancia = new float[1];
        Location.distanceBetween(ponto.getLatitude(), ponto.getLongitude(),
            MonitorGeofence.LATITUDE, MonitorGeofence.LONGITUDE, distancia);
        boolean simulada = Build.VERSION.SDK_INT >= 31 ? ponto.isMock() : ponto.isFromMockProvider();
        posicao.setText(String.format(Locale.forLanguageTag("pt-BR"),
            "Distância até a entrega\n%.0f m • posição %s",
            distancia[0], simulada ? "simulada" : "real"));
    }

    // Quando o estado salvo muda, o Android avisa aqui. Atualiza a tela com atualizarEstado().
    @Override public void onSharedPreferenceChanged(SharedPreferences estado, String chave) {
        atualizarEstado();
    }

    // Lê o estado salvo e atualiza os textos e botões.
    private void atualizarEstado() {
        boolean ativo = preferencias.getBoolean("ativo", false);
        boolean registrado = preferencias.getBoolean("registrado", false);
        monitoramento.setText(ativo ? (registrado ? "Teste ativo" : "Iniciando teste…") : "Teste parado");
        ultimoEvento.setText(preferencias.getString("evento", "Inicie o teste e mova a posição para dentro da área."));
        registrar.setEnabled(!operando && !inicioPendente && !ativo);
        parar.setEnabled(!operando && ativo);
    }

    private void pararMonitoramento() {
        operando = true;
        atualizarEstado();
        MonitorGeofence.parar(getApplicationContext()).addOnCompleteListener(tarefa -> {
            operando = false;
            atualizarEstado();
            if (!tarefa.isSuccessful()) ultimoEvento.setText("Falha ao parar. Confira a localização e tente iniciar outro teste.");
        });
    }

    // Se a autorização for cancelada, permite tentar iniciar novamente.
    private void cancelarInicio() {
        inicioPendente = false;
        atualizarEstado();
    }

    private void mostrarCoordenadas() {
        TextView pontos = new TextView(this);
        Location fora = MonitorGeofence.pontoParaSimular(MonitorGeofence.RAIO * 2.0);
        Location dentro = MonitorGeofence.pontoParaSimular(MonitorGeofence.RAIO / 2.0);
        String coordenadas = String.format(Locale.US,
            "Latitude, longitude\n\nDestino atual\n%.6f, %.6f\nRaio de %.0f m\n\nComece fora, a cerca de %.0f m\n%.6f, %.6f\n\nDepois entre, a cerca de %.0f m\n%.6f, %.6f",
            MonitorGeofence.LATITUDE, MonitorGeofence.LONGITUDE, MonitorGeofence.RAIO,
            MonitorGeofence.RAIO * 2.0, fora.getLatitude(), fora.getLongitude(),
            MonitorGeofence.RAIO / 2.0, dentro.getLatitude(), dentro.getLongitude());
        if (ultimaPosicao != null) {
            coordenadas += String.format(Locale.US, "\n\nPosição recebida do Android\n%.6f, %.6f",
                ultimaPosicao.getLatitude(), ultimaPosicao.getLongitude());
        }
        pontos.setText(coordenadas +
            "\n\nCopie um ponto para o GPS falso. A distância usa a posição recebida do Android até o destino atual.");
        pontos.setTextSize(16);
        int margem = Math.round(20 * getResources().getDisplayMetrics().density);
        pontos.setPadding(margem, margem, margem, margem);
        pontos.setTextIsSelectable(true);
        ScrollView rolagem = new ScrollView(this);
        rolagem.addView(pontos);
        new AlertDialog.Builder(this).setTitle("Pontos do teste").setView(rolagem)
            .setPositiveButton("Fechar", null).show();
    }

    // consulta o endereço sem travar a interface. O teste usa coordenadas mesmo se a busca falhar.
    @SuppressWarnings("deprecation")
    private void buscarEndereco(TextView destino) {
        consultaEndereco.execute(() -> {
            String endereco = null;
            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.forLanguageTag("pt-BR"));
                    List<Address> resultados = geocoder.getFromLocation(
                        MonitorGeofence.LATITUDE, MonitorGeofence.LONGITUDE, 1);
                    if (resultados != null && !resultados.isEmpty()) {
                        endereco = resultados.get(0).getAddressLine(0);
                    }
                } catch (IOException | IllegalArgumentException erro) {
                }
            }
            String textoEndereco = endereco == null || endereco.trim().isEmpty()
                ? "Endereço indisponível. O destino está em Ver coordenadas para simular."
                : "Endereço aproximado da entrega\n" + endereco;
            telaPrincipal.post(() -> {
                if (!isDestroyed()) destino.setText(textoEndereco);
            });
        });
    }

    private TextView texto(LinearLayout tela, String texto, int tamanho) {
        TextView campo = new TextView(this);
        campo.setText(texto);
        campo.setTextSize(tamanho);
        campo.setPadding(0, 12, 0, 12);
        tela.addView(campo);
        return campo;
    }

    // Cria um botão. setOnClickListener liga o toque à ação recebida.
    private Button botao(LinearLayout tela, String texto, Runnable acao) {
        Button botao = new Button(this);
        botao.setText(texto);
        botao.setAllCaps(false);
        botao.setOnClickListener(view -> acao.run());
        tela.addView(botao);
        return botao;
    }

    // Chamado pelo Android ao sair da tela. Interrompe a distância na tela, mantendo a geofence registrada.
    @Override protected void onPause() {
        preferencias.unregisterOnSharedPreferenceChangeListener(this);
        localizacao.removeLocationUpdates(retornoLocalizacao);
        super.onPause();
    }

    // Chamado pelo Android para guardar o início pendente caso a tela precise ser recriada.
    @Override protected void onSaveInstanceState(Bundle salvo) {
        super.onSaveInstanceState(salvo);
        salvo.putBoolean("inicioPendente", inicioPendente);
        salvo.putBoolean("aguardandoConfiguracoes", aguardandoConfiguracoes);
    }

    // Chamado pelo Android ao destruir a tela; libera a busca de endereço.
    @Override protected void onDestroy() {
        consultaEndereco.shutdownNow();
        telaPrincipal.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
