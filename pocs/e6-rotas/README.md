# Rotas PoC

Aplicativo Android independente para demonstrar validação de endereços e geração de rotas. Abra **esta pasta** no Android Studio e execute o módulo `app`. Requer Android 7.0 (API 24) ou superior, internet e localização ativa; não usa Firebase, conta ou chave de API.

## Fluxo

1. A tela começa com dois campos. Digitar um endereço dispara a busca após uma pequena pausa; editar o texto invalida imediatamente o resultado anterior.
2. Somente resultados identificados como Brasil são aceitos. Falhas aparecem em vermelho abaixo do campo. **Ver no mapa** aparece ao lado do campo quando o endereço foi localizado, permitindo conferir o ponto.
3. **+ Adicionar parada** cria novos campos; é possível remover campos mantendo pelo menos dois.
4. **Gerar rota**, fixo no rodapé, exige pelo menos **dois endereços validados**, além da origem. Campos vazios, com erro ou ainda em busca ficam fora da geração.
5. A permissão de localização é solicitada somente ao gerar. Um modal central mostra o progresso e, em seguida, a rota, a sequência das paradas, distância e tempo estimados. Ele não fecha ao arrastar ou tocar fora: use **Fechar** ou o botão Voltar do Android. Fechar o modal cancela uma geração pendente.

## Como a rota é montada

- O Geocoder do Android transforma endereços em coordenadas; o país retornado precisa ser `BR`. O mapa permite conferir a localização, sem garantir a exatidão do número informado.
- A localização atual do telefone é a origem. A PoC não retorna automaticamente a ela.
- O serviço **Table** do OSRM fornece tempos de viagem pelas ruas entre os pontos. O algoritmo usa essa matriz para escolher a ordem das paradas, considerando que os tempos de ida e volta podem ser diferentes.
- Até 12 paradas, programação dinâmica encontra a sequência de menor duração na matriz. Acima disso, usa o vizinho acessível mais próximo: é uma aproximação, sem garantia de ótimo global.
- O serviço **Route** desenha o trajeto pelas ruas na sequência escolhida. O mapa usa osmdroid e tiles OpenStreetMap. Os tempos são estimativas do serviço, sem trânsito em tempo real.
- Esta PoC trata todas as entradas como paradas simples. Não contém pedidos, veículos ou dependências de coleta antes de entrega do marketplace.

Os serviços públicos dependem de rede e disponibilidade. Nenhum trajeto é substituído por linhas retas se o cálculo falhar.

## Organização e plano de implementação

- `Modelos.kt` e `Ordenacao.kt`: dados e lógica de seleção/ordenação, sem Android.
- `Servicos.kt`: busca de endereços, localização atual e chamadas HTTPS ao OSRM, fora da thread da interface.
- `RotasViewModel.kt`: estado dos campos, cancelamento de buscas antigas e geração a partir de um snapshot dos pontos válidos.
- `TelaRotas.kt` e `Mapa.kt`: formulário, botão fixo, painel sobreposto e mapa nativo com recorte e ciclo de vida próprios.
- `app/src/test/.../OrdenacaoTest.kt`: cenários de ordem otimizada, trechos inacessíveis e exclusão de campos inválidos.

As etapas de verificação são testar a lógica isolada, revisar estados de permissão/erro/cancelamento e compilar o projeto Android quando o ambiente permitir.

Verificação nesta sessão: modelos e algoritmo Kotlin compilados e executados isoladamente; 40 matrizes direcionadas comparadas com enumeração exaustiva, além de campos inválidos/editados, trechos sem acesso e aproximação acima de 12 paradas. Revisão de código realizada; painel adaptado para telas baixas com rolagem e altura mínima do mapa. **Build Android e execução no aparelho ainda não realizados**, devido à restrição de acesso ao Gradle já identificada no ambiente. O teste JUnit incluído pode ser executado no Android Studio.

Referências: [OSRM Table/Route](https://project-osrm.org/docs/v5.24.0/api/), [Geocoder Android](https://developer.android.com/reference/android/location/Geocoder), [modal Compose](https://developer.android.com/develop/ui/compose/components/dialog).
