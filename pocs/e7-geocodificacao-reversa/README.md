# Geocodificação Reversa PoC

Aplicativo Android independente que transforma **coordenadas em endereço** (reverse geocoding). Abra **esta pasta** no Android Studio e execute o módulo `app`. Requer Android 7.0 (API 24) ou superior e internet; não usa Firebase, conta ou chave de API.

## Fluxo

1. **Toque no mapa**: o ponto tocado vira o marcador e o endereço correspondente aparece no cartão abaixo.
2. **Minha localização**: pede a permissão de localização, obtém a posição do aparelho e busca o endereço.
3. **Latitude/Longitude**: digite as coordenadas (vírgula ou ponto decimal) e toque em **Buscar endereço**.

Uma nova consulta cancela a anterior, então o cartão nunca mostra o endereço de um ponto antigo.

## Como funciona

- `Geocoder.getFromLocation(lat, lon, 5)` do Android faz a consulta. No Android 13+ usa a versão assíncrona com `GeocodeListener`; antes disso, a versão bloqueante roda em `Dispatchers.IO`.
- Resultados com país `BR` têm prioridade. Um endereço estrangeiro também é exibido, com o aviso de que o app MonkeyMart aceita apenas endereços no Brasil.
- O endereço é montado a partir dos campos do `Address` (rua, número, bairro, cidade, estado, CEP). Sem rua, usa a linha pronta do serviço.
- O resultado é **aproximado**: o serviço devolve o endereço mais próximo do ponto, e o número pode não ser exato.
- Mapa: o mesmo padrão do app e da PoC e5, com osmdroid, tiles OpenStreetMap, pino `ic_pin_monkey`, zoom 18 e balão com o endereço.

## Organização

- `Endereco.kt`: modelos e lógica pura (formatação, escolha do resultado brasileiro, leitura de coordenadas).
- `Servicos.kt`: Geocoder e localização atual.
- `Mapa.kt`: mapa com toque e marcador.
- `Tela.kt`: formulário, botões e cartão de resultado.
- `app/src/test/.../EnderecoTest.kt`: testes da lógica pura.

## Emulador

O emulador começa com a localização padrão do Google (37.4220, -122.0840, Mountain View, EUA). Por isso **Minha localização** mostra um endereço americano. Para testar com o seu endereço, abra os **Extended Controls** do emulador (botão `...`) > **Location**, pesquise o endereço, clique em **Set location** e toque em **Minha localização** de novo. Também dá para rodar em um celular real.

## Verificação

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`: build ok, 6 testes passando.
- Emulador (API 35, Google Play): o toque no mapa perto da Praça da Sé (SP) retornou "Acesso À Praça Doutor João Mendes - Liberdade, São Paulo - SP, 01501-040, Brasil".
- Observação: a primeira consulta logo após ligar o emulador falhou ("network failure") porque o serviço de geocodificação do Google Play ainda estava iniciando; as seguintes funcionaram.

Referência: [Geocoder Android](https://developer.android.com/reference/android/location/Geocoder).
