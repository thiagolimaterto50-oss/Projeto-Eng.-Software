# Estado da entrega parcial — 16/11/2026

## Funcionalidades implementadas

Protótipo MVC com Thymeleaf, controllers, services, entidades e persistência JPA. Fluxo da solicitação: abertura, análise, complementação, orçamento versionado, aprovação/recusa, encaminhamento e confirmação; cancelamento antes de encaminhamento e histórico com autoria.

Incremento de 12/11: login/logout, senhas BCrypt, três perfis reais, isolamento de dados por cliente (incluindo pesquisa e indicadores), mudança de senha, edição e inativação de clientes/aeronaves, justificativa de vínculo, triagem interna e um anexo persistido no banco por solicitação. PDF/PNG/JPEG até 5 MB, com download autenticado e substituição do arquivo atual. A correção do proprietário pelo gestor foi acrescentada em 16/11, conforme registro abaixo.

## Verificação

- `gradlew test bootJar`: 22 testes aprovados (21 integração e 1 contexto).
- Login real, logout, conta inativa, senha pessoal e senhas iniciais com hash.
- URLs e operações de outro cliente negadas; contas/perfis não executam ações alheias.
- Fluxo completo com gestor, cliente e responsável reais, incluindo histórico, versões e validações de datas/valores.
- Edição/inativação/reativação, matrícula duplicada e manutenção do proprietário.
- Upload e download autorizados; rejeição de arquivo vazio, formato inválido, tamanho excessivo e acesso de outro cliente.
- Navegador Edge/Playwright com três sessões reais: abertura, upload/download, triagem, complemento, orçamento, aprovação, encaminhamento, confirmação e logout. Sem erros JavaScript. Conferida largura de celular sem transbordamento da página.
- Capturas locais em `sgma/build/verification/`, ignoradas pelo Git.

## Como apresentar

Siga o roteiro e as credenciais de demonstração no README da raiz. As contas conhecidas são criadas quando o perfil `prod` está desativado. A aplicação local usa MySQL. Cadastros antigos sem senha podem ser habilitados pelo gestor em Clientes → Editar. As ações novas registram o usuário autenticado; eventos antigos simulados permanecem intactos.

## Limitações e próximos cuidados

- Execução local validada com MySQL 8.4.13 e usuário dedicado ao banco. A implantação em servidor permanece pendente.
- Sem recuperação de senha por e-mail, antivírus, múltiplos anexos, histórico de arquivos, exportação ou notificações.
- Criação inicial de gestores/responsáveis por variáveis de ambiente; não há tela administrativa para esses perfis.
- Diagramas originais permanecem como referência; revisar a representação do anexo e os nomes de pacote.
- Conferir o conjunto final de funcionalidades com o documento completo de requisitos.

## Página pública e cadastro de clientes

Apresentação pública em `/`, cabeçalho com login/cadastro, formulário de cadastro de cliente e painel protegido em `/painel`. Cadastro valida confirmação de senha, e-mail, documento, duplicidade e CSRF. Conta é sempre de cliente, mesmo com tentativa de enviar outro perfil. Três testes adicionais cobrem a página pública, cadastro/login e rejeições.

## Verificação da execução local em 15/11/2026

- Windows, JDK 17 e MySQL Community Server 8.4.13, com banco inicialmente vazio e usuário `sgma` restrito ao schema da aplicação.
- `clean test bootJar` concluído com 17 testes aprovados. H2 ficou restrito à suíte automatizada.
- Navegador Edge com login real dos três perfis, abertura, anexo, orçamento, aprovação, encaminhamento e confirmação.
- Cadastro público de um segundo cliente e negativa HTTP 403 ao consultar a solicitação e o anexo do primeiro.
- Consulta SQL confirmou hash BCrypt de 60 caracteres nas quatro contas criadas durante a verificação.
- Reinício do servidor MySQL e da aplicação preservou o login, a solicitação concluída, os sete eventos do fluxo com anexo e o download do arquivo.
- O README inclui preparação do MySQL, variáveis de ambiente, início em Windows/Linux/macOS, contas de avaliação e diagnóstico dos erros comuns. A validação em navegador ocorreu no Windows.


## RF002, RF003 e isolamento — 16/11/2026

- RF002: Minha conta permite editar nome, e-mail, documento e telefone. O ID vem da sessão. A sessão usa ID imutável, para que alterar/reutilizar um e-mail não troque a identidade do usuário. Duplicidade de e-mail/documento e campos inválidos são rejeitados.
- RF003: cliente cadastra aeronave em sua conta; tentativa de indicar outro cliente é negada. Gestor corrige vínculo com justificativa obrigatória, com histórico persistido de clientes anterior/novo, autor e instante. Abertura e correção bloqueiam a mesma aeronave durante a transação.
- Decisão de implementação: solicitações já abertas continuam vinculadas ao cliente original. O novo proprietário recebe a aeronave para solicitações futuras, sem acesso aos atendimentos antigos. Histórico de correções é visível somente ao gestor.
- Falha reproduzida antes da correção: uma referência JPA à classe base Usuario podia não passar em `instanceof Cliente`, fazendo `filtroCliente` retornar null e removendo o filtro de propriedade. AcessoService resolve o tipo real com Hibernate.unproxy e nega perfis desconhecidos. O teste de regressão falhou no código anterior e passou após a correção.
- `gradlew test bootJar`: 22 testes aprovados. Novos testes cobrem o proxy JPA, cadastro com proprietário forjado, autoedição, duplicidades, preservação de identidade após reuso de e-mail e correção auditada sem expor pedidos antigos.
- MySQL 8.4.13 com dados da verificação anterior, Edge/Playwright e três sessões: cadastro de dois clientes, login, cadastro da própria aeronave, abertura, edição de e-mail com sessão ativa, isolamento na listagem/busca/painel/frota e HTTP 403 na ficha alheia. Gestor corrigiu vínculo e consultou o histórico; cliente anterior manteve seu pedido e novo cliente recebeu apenas a aeronave.
- Controles de Minha conta visíveis em 360 e 1280 px. Isto não substitui a revisão de todas as telas exigida por RNF001.
- Reinício da aplicação com o mesmo MySQL preservou o login pelo e-mail atualizado, o proprietário corrigido e seu histórico; a ficha antiga continuou acessível ao cliente original e negada ao novo proprietário.
