# Estado da entrega parcial — 12/11/2026

## Funcionalidades implementadas

Protótipo MVC com Thymeleaf, controllers, services, entidades e persistência JPA. Fluxo da solicitação: abertura, análise, complementação, orçamento versionado, aprovação/recusa, encaminhamento e confirmação; cancelamento antes de encaminhamento e histórico com autoria.

Incremento de 12/11: login/logout, senhas BCrypt, três perfis reais, isolamento de dados por cliente (incluindo pesquisa e indicadores), mudança de senha, edição e inativação de clientes/aeronaves, justificativa de vínculo, triagem interna e um anexo persistido no banco por solicitação. PDF/PNG/JPEG até 5 MB, com download autenticado e substituição do arquivo atual. O proprietário é preservado na edição da aeronave.

## Verificação

- `gradlew test bootJar`: 14 testes aprovados (13 integração e 1 contexto).
- Login real, logout, conta inativa, senha pessoal e senhas iniciais com hash.
- URLs e operações de outro cliente negadas; contas/perfis não executam ações alheias.
- Fluxo completo com gestor, cliente e responsável reais, incluindo histórico, versões e validações de datas/valores.
- Edição/inativação/reativação, matrícula duplicada e manutenção do proprietário.
- Upload e download autorizados; rejeição de arquivo vazio, formato inválido, tamanho excessivo e acesso de outro cliente.
- Navegador Edge/Playwright com três sessões reais: abertura, upload/download, triagem, complemento, orçamento, aprovação, encaminhamento, confirmação e logout. Sem erros JavaScript. Conferida largura de celular sem transbordamento da página.
- Capturas locais em `sgma/build/verification/`, ignoradas pelo Git.

## Como apresentar

Siga o roteiro e as credenciais de demonstração no README da raiz. Contas conhecidas são exclusivas dos perfis demo/test. Cadastros antigos sem senha podem ser habilitados pelo gestor em Clientes → Editar. As ações novas registram o usuário autenticado; eventos antigos simulados permanecem intactos.

## Limitações e próximos cuidados

- A validação realizada usou H2; MySQL e deploy continuam pendentes de verificação no ambiente do grupo.
- Sem recuperação de senha por e-mail, antivírus, múltiplos anexos, histórico de arquivos, transferência de proprietário, exportação ou notificações.
- Criação inicial de gestores/responsáveis por variáveis de ambiente; não há tela administrativa para esses perfis.
- Diagramas originais permanecem como referência; revisar a representação do anexo e os nomes de pacote.
- Conferir o conjunto final de funcionalidades com o documento completo de requisitos, que ainda não foi fornecido nesta conversa.
