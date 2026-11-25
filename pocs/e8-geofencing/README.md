# Entrega 8 · Geofencing

A PoC segue o guia [Criar e monitorar regiões geográficas](https://developer.android.com/develop/sensors-and-location/location/geofencing?hl=pt-br). Usa `Geofence.Builder`, `GeofencingRequest`, `GeofencingClient.addGeofences`, `PendingIntent` e `BroadcastReceiver`. O receiver recebe os eventos `ENTER` e `EXIT` do Google Play Services. A distância exibida é informativa. O disparo do aviso acontece no receiver a partir do evento da API.

## Executar

Abra esta pasta como projeto no Android Studio e execute o módulo `app` no celular ou em um emulador com Google APIs. Compile com SDK 35 e Java 21. O aparelho precisa ter Android 6 ou superior e Google Play Services. A PoC usa uma notificação local e dispensa Firebase.

A tela mostra o endereço aproximado do destino, o raio configurado, a distância e o último evento. O endereço é buscado por geocodificação reversa com `Geocoder`, fora da thread da interface, e pode depender da rede. Se a busca falhar, as coordenadas continuam disponíveis. O destino da geofence permanece o mesmo.

Toque em **Ver coordenadas para simular** para consultar e copiar os pontos. A tela principal tem os botões **Iniciar teste** e **Parar teste**. As permissões são solicitadas ao iniciar.

O destino e o raio são definidos somente em `MonitorGeofence.java`, pelas constantes `LATITUDE`, `LONGITUDE` e `RAIO`. A janela **Ver coordenadas para simular** lê essas constantes e gera automaticamente dois pontos: um a duas vezes o raio, para começar fora, e outro a metade do raio, para entrar.

Se o destino ou o raio mudar entre versões, a PoC invalida o teste antigo. Depois de compilar e reinstalar, toque em **Iniciar teste** para registrar a nova área. Use os pontos mostrados pela versão instalada.

## Repetir a aproximação automática já validada

O arquivo `trajeto-aproximacao.gpx` foi reproduzido no Mock Locations e validado no celular físico em 18/11/2026. O teste confirmou o cadastro da geofence, a redução automática da distância, o evento `ENTER` e a publicação da notificação do Android.

Esse arquivo contém o destino `-21.784000, -48.210700`, com raio de 200 m. Começa a cerca de 400 m, permanece fora por 30 segundos, aproxima-se até 100 m ao longo de 60 segundos e mantém a posição de chegada por mais 60 segundos. Se mudar o destino ou o raio da PoC, será preciso preparar outro GPX.

1. Na PoC, toque em **Parar teste** se houver um teste ativo.
2. No Mock Locations, abra **Reproduzir GPX**. O arquivo **MonkeyMart-aproximacao.gpx** já foi copiado para **Downloads** do celular e carregado durante a validação. Se precisar importar novamente, use o botão de importação e escolha o arquivo.
3. Toque no **triângulo verde de reprodução**, no canto superior esquerdo do Mock Locations.
4. Volte à PoC durante os primeiros 30 segundos. Confira a posição simulada perto de 400 m e toque em **Iniciar teste**.
5. Aguarde o movimento automático. Na validação, o evento de entrada chegou quando a tela estava perto de 189 m. A posição depois chegou a 100 m e a notificação **Entregador por perto** foi confirmada.
6. Ao terminar, toque em **Parar teste** na PoC. Encerre também a reprodução no Mock Locations se ela ainda estiver ativa.

O modo de um único ponto do Mock Locations fica estacionado. O valor de percurso e velocidade pode ficar zerado nesse modo. Na aba **Reproduzir GPX**, o arquivo pronto fornece o deslocamento automaticamente e dispensa o cálculo de rota pela rede.

## Testar com GPS falso no celular

1. Nas opções do desenvolvedor do celular, selecione o seu aplicativo de GPS falso como **app de localização simulada**.
2. No GPS falso, coloque a posição no ponto **Comece fora** da janela de coordenadas e inicie o fornecimento da localização. Mantenha o aplicativo de GPS falso funcionando enquanto alterna entre os apps.
3. Abra a PoC e toque em **Iniciar teste**. A PoC solicita as permissões que estiverem faltando, em sequência. Conceda localização precisa.
4. No Android 10, conceda também a permissão de segundo plano quando solicitada. No Android 11 ou superior, o app explica como abrir **Permissões → Localização → Permitir o tempo todo** nas configurações. Ao voltar com a permissão concedida, a PoC continua o início do teste.
5. No Android 13 ou superior, aceite a solicitação de notificações que aparecerá em seguida. Se negar, o monitoramento ainda funciona e mostra o evento na tela. A notificação na barra depende dessa autorização.
6. Aguarde a confirmação **Teste ativo**. Confira se a posição recebida corresponde ao ponto de fora e se a distância está próxima de 400 m. Cancelar ou negar a localização interrompe o início do teste. Para tentar novamente, toque em **Iniciar teste**. Se o Android deixar de mostrar a solicitação após recusas, ajuste a permissão nas configurações do aplicativo.
7. Volte ao GPS falso e mude para o ponto **Depois entre** da janela de coordenadas. Aguarde o evento e retorne à PoC. Deve aparecer **Entrada (ENTER)** e uma notificação do Android.
8. Mova para fora e aguarde **EXIT**. Mova para dentro outra vez e aguarde a reentrada. O receiver registra a transição e evita outro aviso para o mesmo teste.
9. Use **Parar teste** e depois **Iniciar teste** para começar um novo teste.

O provedor usado para receber posições é `FusedLocationProviderClient`, compatível com posições simuladas fornecidas pelo Android. O funcionamento do aplicativo externo de GPS falso precisa ser validado no aparelho. O rótulo **Posição simulada** aparece quando o Android identifica a posição dessa forma.

## Testar sem GPS falso

No emulador, use **Extended Controls (⋮) → Location**. Defina o ponto de fora com **Set location**, registre o perímetro na PoC e depois defina o ponto de dentro. Também é possível reproduzir uma rota em **Routes → Play route**.

No celular com GPS real, é preciso se deslocar até a região do destino configurado e atravessar o raio. Para mudar o local, altere `LATITUDE` e `LONGITUDE` em `MonitorGeofence.java` e compile/reinstale. O endereço, os pontos de simulação e o cálculo de distância usam o mesmo destino. O raio da tela também vem de `RAIO`.

## Comportamento dos eventos

A API pode levar alguns minutos para entregar as transições. A posição e a distância na tela podem mudar antes de o receiver receber o evento. `setNotificationResponsiveness(5000)` é uma preferência de resposta e não garante entrega em cinco segundos. Mantenha a localização e os serviços do Google ativos. Uma conexão com a internet pode ser necessária para a localização em rede e os serviços envolvidos.

O pedido usa `INITIAL_TRIGGER_ENTER`. Se a posição já estiver dentro do raio ao registrar, a API também pode emitir `ENTER`. Para demonstrar o cruzamento, comece no ponto de fora.

O monitoramento continua registrado ao trocar de aplicativo, para permitir operar o GPS falso. Ao terminar, toque em **Parar teste**. Depois de reiniciar o aparelho, pare e registre o perímetro novamente. O aviso é registrado uma vez por teste e persistido localmente, inclusive quando a tela está fechada. Se a permissão de notificações estiver bloqueada, a tela informa que a aproximação foi registrada.

## Arquivos da PoC

- `MainActivity.java` monta a tela, solicita permissões, inicia/para o monitoramento e exibe a posição informativa.
- `MonitorGeofence.java` define destino e raio, cria o pedido e registra/remove a geofence.
- `GeofenceReceiver.java` recebe as transições, evita aviso repetido e publica a notificação.
- `AndroidManifest.xml` declara permissões e receiver.

## Demonstrar no MonkeyMart dentro da sala

Instale o APK debug do projeto principal em dois celulares Android e use as contas de comprador e entregador. A versão debug mostra **Simular aproximação** em cada parada de entrega. O botão simula posições relativas ao destino escolhido e funciona com os celulares parados.

1. No comprador, faça uma compra e mantenha o app aberto. Use um endereço de entrega mais distante que 200 m da sala, para observar o aviso sendo disparado pela simulação.
2. No vendedor, marque o pedido como pronto para retirada.
3. No entregador, selecione o pedido, gere a rota, selecione o veículo e toque em **Iniciar entregas**. Gerar a rota ainda exige a localização atual do aparelho e conexão com o serviço de rotas.
4. Confirme a coleta. Na parada de **ENTREGA**, toque em **Simular aproximação**. A tela mostra posições a 350 m, 250 m e 150 m do destino, em intervalos de dois segundos.
5. No passo de 150 m, o detector de proximidade salva o aviso no Room. Aguarde a sincronização com o Firestore, que tenta reenviar os pedidos a cada 15 segundos. Confira o aviso no celular do comprador e toque em **Ver compras**.
6. O botão fica desabilitado para os pedidos já avisados. Para repetir a apresentação, crie outro pedido. Confirme a entrega e confira que a mensagem deixa de aparecer no pedido.

Mantenha os dois apps abertos e conectados à internet para a demonstração entre contas. A simulação usa o mesmo detector e a mesma gravação local do monitoramento real. Ao sair da tela da rota ou colocar o app em segundo plano, a simulação para. Um aviso que já foi salvo continua aguardando sincronização.

Com um celular, faça os passos do entregador, aguarde a sincronização e entre como comprador para conferir o aviso em **Minhas compras**. Para testar o GPS real, mantenha o app aberto e atravesse o raio de 200 m depois da coleta.

Para testar sem conexão, inicie a rota com internet e confirme a coleta. Desconecte o entregador, execute a simulação e reconecte. O evento fica no Room até a próxima sincronização.

### Firestore

O app usa o projeto Firebase já configurado e grava `avisoProximidade: true` no documento existente em `pedidos/{firestoreId}`. Documentos antigos sem o campo são lidos como `false`. Confira se as regras publicadas aceitam esse campo, especialmente se houver lista de campos permitidos. A transição de `false` ou ausente para `true` deve ser restrita ao entregador atribuído a um pedido coletado em rota. A tela do comprador consulta o Room pelo e-mail autenticado. O acesso remoto ao documento segue as regras já publicadas para `pedidos`.
