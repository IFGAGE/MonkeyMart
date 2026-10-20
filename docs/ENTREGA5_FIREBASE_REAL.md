# Validação no Firebase real

Data: 06/10/2026. Projeto: marketplace-15012. Java 21.

Teste instrumentado passou (1 teste com múltiplos cenários). Foram usadas as funções de produção sincronizarPedidos e acompanharPedidos, com dois bancos Room independentes no mesmo emulador isolado. Leituras Source.SERVER confirmaram os dados na nuvem, e não apenas no cache do SDK.

Perfis configurados: edu@gmail.com = negociante (cliente); gabriel@gmail.com = entregador. Campos existentes foram preservados. Campos ausentes receberam dados fictícios de teste, sem pretensão de representar dados pessoais. A configuração foi feita no documento usuarios de cada conta; o formulário da interface não foi executado neste teste.

`	ext
Projeto: marketplace-15012
Execução: e266371e-cf42-4488-baf0-2db4f585ef2c
Autenticação cliente: OK
Perfil edu@gmail.com = negociante: confirmado no servidor
Autenticação entregador: OK
Perfil gabriel@gmail.com = entregador: confirmado no servidor
Autenticação cliente: OK
Pedido 40f1c63a-e245-49f5-99d2-04e370f0a70f: enviado e confirmado por Source.SERVER
Autenticação entregador: OK
Recebimento no Room do entregador: OK
Status ENTREGUE pelo entregador: confirmado no servidor
Autenticação cliente: OK
Retorno do status ao Room do cliente: OK
Pedido offline f29e2020-b18b-4ec1-8201-024e17f8ecd0: preservado localmente e reenviado ao servidor
RESULTADO: TODOS OS CENÁRIOS PASSARAM. Dois pedidos de teste mantidos como evidência.
`

Os dois pedidos novos foram mantidos como evidência, com resumo TESTE ENTREGA 5 ou TESTE OFFLINE ENTREGA 5. Não foram atualizados pedidos preexistentes. O teste não altera regras de segurança do Firebase.

Limites: o teste valida autenticação, gravação, recepção, status e reenvio usando o código de sincronização de produção. Não constitui teste completo do checkout pela interface nem execução em dois aparelhos físicos. O perfil é consultado pelo app no Room local; configurar o documento remoto não preenche automaticamente o perfil em instalações que ainda não o cadastraram.
