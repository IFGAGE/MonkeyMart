# Entrega 8 — acompanhamento e aproximação automáticos

O app principal acompanha a posição do entregador durante os pedidos em andamento de uma rota iniciada. O comprador abre **Minhas compras → Acompanhar entrega** para ver o mapa, o marcador do entregador e a distância em metros. A distância é em linha reta até o endereço, como na PoC; a linha azul no mapa é o trajeto por ruas consultado separadamente.

## O que foi corrigido

Antes, havia registro de geofences e aviso por evento, mas não existia compartilhamento contínuo das posições nem uma tela de acompanhamento para o comprador. Alterações nas flags podiam provocar novo registro das áreas, fazendo o evento inicial aparecer junto de uma interação.

Agora o caminho que atualiza os metros e registra a aproximação recebe posições diretamente do Android, assim que a rota é iniciada, independentemente dos checks de coleta e entrega. Marcar **entregue** conclui aquele pedido e encerra o acompanhamento.

## Fluxo no código

1. `ControleLocalizacaoDaRota` solicita as permissões. `ProximidadeEntrega.monitorarDestinos` acompanha a rota iniciada e os pedidos em andamento.
2. Com a tela ativa e localização autorizada, inicia `ServicoRastreamento`, um serviço de localização em primeiro plano com notificação persistente. Ele continua recebendo posições ao abrir o Mock Locations ou apagar a tela.
3. `observarLocalizacao` usa `FusedLocationProviderClient`, a mesma fonte da PoC. Recebe posições reais ou simuladas; não movimenta o celular artificialmente.
4. Para cada ponto atual e com precisão informada até 100 m, `processarPosicaoEntrega` carrega os pedidos atuais da rota, calcula as distâncias e publica os pontos para o acompanhamento.
5. Se a distância entra nos 200 m, registra o aviso no Room usando a transação `registrarEntradaGeofence`. A mesma transação confere a rota, as coordenadas e o estado do pedido e a regra de aviso único. A confirmação de entrega não chama esse caminho.
6. O geofencing nativo continua cadastrado como apoio. Eventos `ENTER` também podem registrar a aproximação. Os dois caminhos compartilham a mesma proteção contra duplicação.
7. A posição mais recente é compartilhada no campo `rastreamento` do documento existente `pedidos/{firestoreId}`. Isso não altera status, coleta ou revisão. A sincronização normal dos pedidos usa merge para preservar esse campo.
8. `AcompanharEntregaScreen` recebe atualizações desse documento por listener do Firestore. Não usa a localização do celular do comprador. Confere entregador, destino e estado antes de exibir o ponto.
9. O mapa mantém seus componentes e move o marcador a cada ponto. O trajeto por ruas é recalculado separadamente quando há deslocamento; uma falha no serviço de rotas não interrompe o cálculo dos metros.
10. Posições com mais de 30 segundos são identificadas como última posição conhecida, em vez de aparentarem acompanhamento ao vivo. Encerrar os pedidos, a rota ou sair da conta interrompe o serviço.

O aviso ao comprador continua dentro do aplicativo. Não foi adicionado push por FCM com o app fechado. A notificação no celular do entregador confirma o registro local, sem afirmar que o comprador já recebeu a sincronização.

## Teste em um celular com Mock Locations

1. Instale a versão **debug atualizada**. Prepare um pedido novo, marque-o como pronto e entre na conta do entregador.
2. Planeje e inicie a rota. Autorize localização precisa, acesso o tempo todo e notificações. Mantenha o GPS ligado.
3. Em **Ver rota planejada** ou **Ver entregas**, expanda cada parada e abra **Testar localização** para consultar as coordenadas.
4. Deixe os checks de **Coleta** e **Entrega** desmarcados durante o teste do aviso automático.
5. Na parada de **Entrega**, toque em **Ver prévia do comprador**. Para testar a aproximação manualmente, use **Fora (~400 m)** e depois **Dentro (~100 m)** da entrega. Para ver o movimento gradual, reproduza uma rota que termine no endereço de entrega.
6. Confira o aviso automático quando a posição entra nos 200 m. Para testar os checks separadamente, simule primeiro a posição da coleta para confirmá-la e depois volte à posição da entrega para concluí-la.
7. Encerre a simulação. Para repetir o aviso, use outro pedido, pois ele é único por pedido.

Os checks individuais e da parada inteira só ficam disponíveis com uma posição recente dentro de 200 m do respectivo endereço. Para confirmar entrega, a coleta daquele pedido também precisa estar confirmada.

A prévia usa as posições recebidas pelo serviço e indica se a posição é real ou simulada; não inventa um contador de metros nem marca a aproximação por botão. Ela permite testar o movimento e o aviso num aparelho, sem afirmar que há outra conta de comprador conectada simultaneamente.

Para validar duas contas em tempo real, o comprador abre **Minhas compras → Acompanhar entrega** em outra sessão/aparelho enquanto o entregador continua conectado. O app avisa antes de sair da conta do entregador com a rota em andamento. A saída pausa o compartilhamento até entrar novamente nessa conta. No mesmo celular, use **Ver prévia do comprador** enquanto o Mock Locations reproduz a rota. A prévia permite observar o movimento sem trocar de conta.

## Conexão e permissões do Firebase

A prévia local continua mostrando as posições sem internet. O compartilhamento entre aparelhos, os mapas e o trajeto por ruas dependem de conexão. O Firestore precisa permitir ao entregador atualizar o campo `rastreamento` dos pedidos atribuídos a ele e ao comprador ler seu pedido. Nenhuma regra remota foi alterada automaticamente. Se o projeto restringe os campos atualizáveis, esse campo precisa ser incluído na regra existente; erros de recebimento são mostrados na tela.

Após reiniciar o celular ou forçar a parada, reabra o MonkeyMart para restabelecer o rastreamento.

Referências: [Geofencing](https://developer.android.com/develop/sensors-and-location/location/geofencing?hl=pt-br), [serviço de localização em primeiro plano](https://developer.android.com/develop/background-work/services/fgs/service-types#location), [atualizações em tempo real do Firestore](https://firebase.google.com/docs/firestore/query-data/listen).
