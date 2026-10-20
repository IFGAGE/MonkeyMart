# Roteiro de apresentação - 5 a 6 minutos

## 1. Objetivo - 30 segundos

“A entrega 5 do Marketplace pede a visualização do ponto de entrega no mapa. Desenvolvi uma POC separada para validar endereço, coordenadas e marcador. Depois adaptei esse conhecimento ao pedido do aplicativo principal. Aqui fazemos endereço para coordenadas; o caminho inverso, coordenadas para endereço, é a entrega 7.”

## 2. POC: o que acontece ao buscar - 1 minuto

Abra `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/java/com/example/mapapoc/MainActivity.kt`.

- Linhas 26–28: o texto digitado e o estado da busca são guardados pela tela. `remember` mantém esses valores durante suas atualizações; `mutableStateOf` avisa ao Compose quando precisa redesenhar.
- Linhas 42–49: o botão captura o endereço naquele momento, passa para carregamento e inicia a busca numa coroutine. Assim, a busca não bloqueia a interface.
- Linhas 55–59: o estado escolhe o que mostrar: orientação inicial, carregamento, mapa ou erro.

“Separei o endereço digitado do endereço pesquisado. Se eu editar o campo sem buscar novamente, o marcador continua com o endereço correspondente às coordenadas anteriores. Se a próxima busca falhar, aparece um erro em vez de mostrar o ponto antigo como se fosse válido.”

## 3. Busca: de texto para coordenadas - 1 minuto

Abra `mapa/BuscaEndereco.kt`, no mesmo pacote da POC.

- Linhas 7–13: `Coordenadas` reúne latitude e longitude. `EstadoBuscaEndereco` permite apenas um estado por vez. `Encontrado` guarda juntos o endereço usado e seu ponto.
- Linhas 17–29: a função remove espaços, rejeita texto vazio e chama o serviço, com limite de espera.
- Linhas 31–36: diferencia timeout, cancelamento e falha de consulta. Cancelamento da tela não vira um falso erro de endereço.

Abra `mapa/Geocoding.kt`.

- Linhas 15–18: verifica se o dispositivo tem Geocoder e configura o idioma.
- Linhas 19–29: em Android 13 ou superior, usa uma resposta assíncrona. Quando chegam os resultados, a coroutine continua.
- Linhas 31–35: em versões antigas, usa a chamada compatível numa thread para operações de entrada e saída.
- Linhas 37–39: extrai latitude e longitude do primeiro resultado.

“Geocoder é o componente Android que encontra as coordenadas. OSMDroid é a biblioteca que desenha o mapa. Não estou capturando GPS nem calculando rotas nesta entrega.”

## 4. Mapa e problema de bloqueio - 1 minuto

Abra `mapa/MapaDestino.kt`.

- Linhas 25–35: cria o mapa uma vez, identifica o aplicativo com User-Agent, usa HTTPS, permite gestos, define zoom e mostra o crédito do OpenStreetMap.
- Linhas 38–43: cria o marcador com sua ponta ancorada na coordenada.
- Linhas 45–60: acompanha pausa e retomada da tela e libera recursos ao sair.
- Linhas 61–73: `AndroidView` coloca a View da OSMDroid dentro do Compose. Seu `update` posiciona o marcador e centraliza o mapa no resultado atual.

“Os quadrados do mapa são imagens chamadas tiles. No desenvolvimento apareceram mensagens de bloqueio no lugar dessas imagens. O servidor precisa identificar quem as solicita. Configurei um User-Agent próprio antes de criar o mapa, com nome e versão do aplicativo. Ele identifica a origem; não é um certificado de segurança. O e-mail fictício foi removido. Na investigação também reinstalei o aplicativo para eliminar dados locais antigos, mas não tenho logs daquele momento para provar a causa exata do bloqueio ou do cache.”

## 5. Adaptação ao aplicativo principal - 1 minuto e meio

Abra `app/app/src/main/java/com/example/marketplace/ui/navigation/AppNav.kt`.

- Linhas 234–246: cria o pedido com seu endereço, salva primeiro no Room e inicia o envio à nuvem em separado. A confirmação não depende da internet.
- Linha 327: ao escolher visualizar o mapa, envia o endereço codificado na navegação. Isso preserva caracteres especiais.

Abra `ui/screens/entregador/MapaEntregaScreen.kt`.

- Linhas 44–55: recebe o ID e o endereço do pedido e inicia a mesma busca automaticamente. Na POC havia campo e botão; aqui o endereço já vem do pedido.
- Linhas 108–114: mostra mapa, carregamento ou erro com opção de tentar novamente.

“A maior mudança foi a origem do endereço: na POC ele vem do campo de teste; no principal vem do pedido salvo. Reutilizei a lógica de busca e apresentação, adaptando a interface e o ícone ao MonkeyMart.”

Abra `data/sync/FirestoreSync.kt`.

- Linhas 72–81: envia pedidos locais ainda não sincronizados.
- Linhas 84–96: recebe snapshots da nuvem.
- Linhas 99–131: mantém a recepção e novas tentativas de envio enquanto o usuário está autenticado.

“O manual dispensou nuvem somente para as fotos. Os pedidos continuam no Firestore. Antes existia o envio, mas faltava receber os pedidos de outro dispositivo. Agora o Firestore alimenta o Room, e a interface continua lendo o banco local. Cada novo pedido tem um UUID para não colidir com o número local de outro celular. A migração preserva os pedidos antigos. Alterações offline ainda pendentes não são substituídas por uma versão antiga recebida da nuvem.”

## 6. Validação - 20 segundos

“Compilei a POC e o principal com Java 21. Testei a lógica de busca e sincronização, além de executar testes Android num emulador separado com Firebase local. Validei a preservação de pedidos antigos, troca de dados entre dois caches, reenvio offline, geocodificação e marcador. Também validei envio, recepção, status e reenvio no Firebase real com duas contas de teste.”

Na demonstração, use endereço completo com cidade e estado. Mostre primeiro uma busca na POC e depois um pedido na área do entregador. Não prometa precisão absoluta do Geocoder nem funcionamento de mapas novos sem internet.
