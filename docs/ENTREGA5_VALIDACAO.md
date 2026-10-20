# Entrega 5: alterações e validação

Data: 06/10/2026. Branch: entrega5. Validação realizada antes do commit.

## Escopo e manual

O manual, página 3, exige dados estruturados primeiro no SQLite e depois no Firebase, de forma assíncrona. A página 6 mantém a sincronização com Firestore na entrega 3 e dispensa somente Firebase Storage para as fotos da entrega 4. A entrega 5 pede o ponto de entrega no mapa; reverse geocoding fica para a entrega 7.

As fotos continuam no filesystem local. Não foi adicionada dependência do Firebase Storage. O Firestore já recebia os pedidos; agora seus snapshots também alimentam o Room usado pela interface.

## Alterações

- Busca de endereço com estados exclusivos: inicial, carregando, encontrado ou erro. Uma falha substitui o resultado anterior.
- Endereço pesquisado e coordenadas ficam juntos. Editar o campo não muda o título do ponto anterior.
- Geocoder assíncrono no Android 13+; caminho compatível em Dispatchers.IO para versões anteriores. Tratamento de indisponibilidade, erros e limite de espera.
- Mapa com URL HTTPS oficial, User-Agent identificando somente app/versão (sem e-mail fictício), marcador atualizado e atribuição OSM.
- Eventos de pausa/retomada e liberação do MapView acompanhando a tela.
- Teclado da POC fechado ao buscar; mapa limitado à área restante da tela para não invadir o campo e o botão.
- Endereço codificado com Uri.encode na navegação e botão para tentar novamente após falha.
- Checkout salva localmente antes da confirmação; voltar não cria outro pedido. Envio à nuvem ocorre em separado.
- Novos pedidos usam UUID como firestoreId, evitando colisões entre IDs numéricos de dispositivos. O número local continua sendo usado nas telas.
- Migração 3→4 preserva dados e o vínculo dos pedidos antigos com documentos numéricos.
- Listener remoto atualiza/inclui pedidos no Room. Alterações locais ainda não enviadas são preservadas; envio é serializado, com novas tentativas.
- JDK 21 definido para o daemon Gradle do principal. targetSdk alinhado ao compileSdk 35 já usado.
- Arquivos gerados pelo IDE removidos somente do índice Git, mantendo as cópias locais. Ignore convertido para UTF-8 e ampliado.

## Ambiente

Builds em cópias de principal e POC dentro de validacao-entrega5, com Java 21, dois workers e heap limitado. Android virtual separado, sem janela, porta 5580. Firebase Emulator Suite: projeto demo-monkeymart, Auth 19099 e Firestore 18080. A validação inicial foi local. Depois, a pedido do usuário, houve validação no Firebase real; veja ENTREGA5_FIREBASE_REAL.md. Os testes instrumentados configuram esses serviços apenas pelo runner de testes.

## Testes unitários

| Projeto | Suite | Testes | Falhas | Erros |
|---|---|---:|---:|---:|
| principal | com.example.marketplace.data.sync.PedidoRemotoTest | 7 | 0 | 0 |
| principal | com.example.marketplace.ExampleUnitTest | 1 | 0 | 0 |
| principal | com.example.marketplace.ui.mapa.BuscaEnderecoTest | 7 | 0 | 0 |
| poc | com.example.mapapoc.ExampleUnitTest | 1 | 0 | 0 |
| poc | com.example.mapapoc.mapa.BuscaEnderecoTest | 7 | 0 | 0 |

Os testes relevantes cobrem endereço vazio, sucesso, erro depois de sucesso, falha de rede, timeout, cancelamento, nova busca, IDs únicos, documentos legados/incompletos, atualização de status e preservação de alteração offline.

Resultado: **23 testes unitários e 7 testes Android passaram**, além da repetição visual do mapa principal. Ambos os aplicativos compilaram com Java 21.

## Testes Android e evidências

- Principal: instrumentacao-principal.log. Inclui migração do banco, dois caches com mesmo ID local, envio/recebimento, status entregue, gravação offline seguida de reenvio, geocodificação real e mapa com marcador/atribuição.
- POC: instrumentacao-poc.log. Inclui busca de São Paulo, edição sem alterar o título do resultado e nova busca por Brasília.
- Compilação principal: build-principal-final.log.
- Compilação POC: build-poc-final.log e build-poc-instrumentacao.log.
- Capturas: mapa-principal.png e mapa-poc.png, quando disponíveis, na pasta de validação.

Tentativas iniciais da POC falharam no seletor de acessibilidade e na incompatibilidade do Espresso 3.5.1 com o Android instalado. O teste passou a usar a API do Compose e Espresso 3.7.0, que corrige a chamada interna removida. A execução final está registrada no log acima.

## Limites

As operações de autenticação, perfis e pedidos também passaram no Firebase real com as contas autorizadas; veja ENTREGA5_FIREBASE_REAL.md. Outras permissões e coleções não foram auditadas. A simulação entre dispositivos usa dois bancos Room independentes no mesmo Android virtual e o serviço Firestore real do Emulator Suite. Não é teste em dois aparelhos físicos. Geocoder e tiles foram consultados online em visualização interativa. As versões Android antigas não foram executadas.

Pedidos antigos preservam os IDs numéricos legados. Se dois dispositivos já sobrescreveram um documento antes desta correção, os dados perdidos não podem ser reconstruídos pela migração. Novos pedidos têm IDs próprios.

O tratamento de exclusões remotas e conflitos simultâneos entre dois usuários não foi ampliado; o fluxo validado é criação, recepção e atualização de status. A revisão não substitui a avaliação de outras entregas do projeto.

## Arquivos de código/configuração criados ou modificados

- `.gitignore`
- `app/app/build.gradle.kts`
- `app/app/src/androidTest/assets/schema3.sql`
- `app/app/src/androidTest/java/com/example/marketplace/Entrega5IntegrationTest.kt`
- `app/app/src/androidTest/java/com/example/marketplace/Entrega5TestRunner.kt`
- `app/app/src/androidTest/java/com/example/marketplace/ExampleInstrumentedTest.kt`
- `app/app/src/debug/AndroidManifest.xml`
- `app/app/src/main/java/com/example/marketplace/data/AppDatabase.kt`
- `app/app/src/main/java/com/example/marketplace/data/PedidoMigration.kt`
- `app/app/src/main/java/com/example/marketplace/data/dao/PedidoDao.kt`
- `app/app/src/main/java/com/example/marketplace/data/entity/PedidoEntity.kt`
- `app/app/src/main/java/com/example/marketplace/data/sync/FirestoreSync.kt`
- `app/app/src/main/java/com/example/marketplace/data/sync/PedidoRemoto.kt`
- `app/app/src/main/java/com/example/marketplace/ui/mapa/BuscaEndereco.kt`
- `app/app/src/main/java/com/example/marketplace/ui/mapa/Geocoding.kt`
- `app/app/src/main/java/com/example/marketplace/ui/mapa/MapaDestino.kt`
- `app/app/src/main/java/com/example/marketplace/ui/navigation/AppNav.kt`
- `app/app/src/main/java/com/example/marketplace/ui/screens/entregador/MapaEntregaScreen.kt`
- `app/app/src/main/java/com/example/marketplace/ui/screens/negociante/CarrinhoScreen.kt`
- `app/app/src/test/java/com/example/marketplace/data/sync/PedidoRemotoTest.kt`
- `app/app/src/test/java/com/example/marketplace/ui/mapa/BuscaEnderecoTest.kt`
- `app/gradle/gradle-daemon-jvm.properties`
- `docs/ENTREGA5_APRESENTACAO.md`
- `docs/ENTREGA5_VALIDACAO.md`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/build.gradle.kts`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/androidTest/java/com/example/mapapoc/ExampleInstrumentedTest.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/androidTest/java/com/example/mapapoc/MapaPocIntegrationTest.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/AndroidManifest.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/java/com/example/mapapoc/MainActivity.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/java/com/example/mapapoc/mapa/BuscaEndereco.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/java/com/example/mapapoc/mapa/Geocoding.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/main/java/com/example/mapapoc/mapa/MapaDestino.kt`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/src/test/java/com/example/mapapoc/mapa/BuscaEnderecoTest.kt`

## Arquivos gerados retirados somente do versionamento

- `.idea/.gitignore`
- `.idea/AndroidProjectSystem.xml`
- `.idea/DDM2.iml`
- `.idea/appInsightsSettings.xml`
- `.idea/caches/deviceStreaming.xml`
- `.idea/deploymentTargetSelector.xml`
- `.idea/gradle.xml`
- `.idea/markdown.xml`
- `.idea/misc.xml`
- `.idea/modules.xml`
- `.idea/runConfigurations.xml`
- `.idea/vcs.xml`
- `app/.idea/.name`
- `app/.idea/AndroidProjectSystem.xml`
- `app/.idea/compiler.xml`
- `app/.idea/deploymentTargetSelector.xml`
- `app/.idea/deviceManager.xml`
- `app/.idea/gradle.xml`
- `app/.idea/inspectionProfiles/Project_Default.xml`
- `app/.idea/migrations.xml`
- `app/.idea/misc.xml`
- `app/.idea/runConfigurations.xml`
- `app/.idea/vcs.xml`
- `app/.kotlin/errors/errors-1790118564250.log`
- `app/.kotlin/errors/errors-1790706382901.log`
- `app/app/.idea/.gitignore`
- `app/app/.idea/AndroidProjectSystem.xml`
- `app/app/.idea/caches/deviceStreaming.xml`
- `app/app/.idea/gradle.xml`
- `app/app/.idea/migrations.xml`
- `app/app/.idea/misc.xml`
- `app/app/.idea/modules.xml`
- `app/app/.idea/runConfigurations.xml`
- `app/app/.idea/vcs.xml`
- `pocs/.idea/.gitignore`
- `pocs/.idea/.name`
- `pocs/.idea/AndroidProjectSystem.xml`
- `pocs/.idea/caches/deviceStreaming.xml`
- `pocs/.idea/compiler.xml`
- `pocs/.idea/deploymentTargetSelector.xml`
- `pocs/.idea/gradle.xml`
- `pocs/.idea/misc.xml`
- `pocs/.idea/modules.xml`
- `pocs/.idea/pocs.iml`
- `pocs/.idea/runConfigurations.xml`
- `pocs/.idea/vcs.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/.gitignore`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/.name`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/AndroidProjectSystem.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/compiler.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/deploymentTargetSelector.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/gradle.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/kotlinc.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/migrations.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/misc.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/runConfigurations.xml`
- `pocs/e1-auth/Autenticacao_gabriel_ana_edu_gabi/.idea/vcs.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/.gitignore`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/AndroidProjectSystem.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/compiler.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/deploymentTargetSelector.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/deviceManager.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/gradle.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/inspectionProfiles/Project_Default.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/misc.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/runConfigurations.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/studiobot.xml`
- `pocs/e2-sqlite/gabriel/Cadastros/.idea/vcs.xml`
- `pocs/e3-cache/Ana/.idea/.gitignore`
- `pocs/e3-cache/Ana/.idea/.name`
- `pocs/e3-cache/Ana/.idea/compiler.xml`
- `pocs/e3-cache/Ana/.idea/deploymentTargetSelector.xml`
- `pocs/e3-cache/Ana/.idea/gradle.xml`
- `pocs/e3-cache/Ana/.idea/kotlinc.xml`
- `pocs/e3-cache/Ana/.idea/migrations.xml`
- `pocs/e3-cache/Ana/.idea/misc.xml`
- `pocs/e3-cache/Ana/.idea/runConfigurations.xml`
- `pocs/e3-cache/Ana/.idea/vcs.xml`
- `pocs/e3-cache/Ana/app/.idea/caches/deviceStreaming.xml`
- `pocs/e3-cache/Ana/app/.idea/gradle.xml`
- `pocs/e3-cache/Ana/app/.idea/migrations.xml`
- `pocs/e3-cache/Ana/app/.idea/misc.xml`
- `pocs/e3-cache/Ana/app/.idea/runConfigurations.xml`
- `pocs/e3-cache/Ana/app/.idea/vcs.xml`
- `pocs/e3-cache/Ana/app/.idea/workspace.xml`
- `pocs/e4-storage/gabi/.idea/.gitignore`
- `pocs/e4-storage/gabi/.idea/.name`
- `pocs/e4-storage/gabi/.idea/AndroidProjectSystem.xml`
- `pocs/e4-storage/gabi/.idea/compiler.xml`
- `pocs/e4-storage/gabi/.idea/deploymentTargetSelector.xml`
- `pocs/e4-storage/gabi/.idea/gradle.xml`
- `pocs/e4-storage/gabi/.idea/misc.xml`
- `pocs/e4-storage/gabi/.idea/runConfigurations.xml`
- `pocs/e4-storage/gabi/.idea/vcs.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/AndroidProjectSystem.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/compiler.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/deploymentTargetSelector.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/deviceManager.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/gradle.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/misc.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/runConfigurations.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/.idea/vcs.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/.gitignore`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/AndroidProjectSystem.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/caches/deviceStreaming.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/gradle.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/migrations.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/misc.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/runConfigurations.xml`
- `pocs/e5-mapa/Eduardo/MapaPoc/app/.idea/vcs.xml`
