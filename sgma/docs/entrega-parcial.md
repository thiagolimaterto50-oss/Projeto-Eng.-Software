# Estado da entrega parcial — 15/11/2026

## Funcionalidades implementadas

Protótipo MVC com Thymeleaf, controllers, services, entidades e persistência JPA. Fluxo da solicitação: abertura, análise, complementação, orçamento versionado, aprovação/recusa, encaminhamento e confirmação; cancelamento antes de encaminhamento e histórico com autoria.

Incremento de 12/11: login/logout, senhas BCrypt, três perfis reais, isolamento de dados por cliente (incluindo pesquisa e indicadores), mudança de senha, edição e inativação de clientes/aeronaves, justificativa de vínculo, triagem interna e um anexo persistido no banco por solicitação. PDF/PNG/JPEG até 5 MB, com download autenticado e substituição do arquivo atual. O proprietário é preservado na edição da aeronave.

## Verificação

- `gradlew test bootJar`: 17 testes aprovados (16 integração e 1 contexto).
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
- Sem recuperação de senha por e-mail, antivírus, múltiplos anexos, histórico de arquivos, transferência de proprietário, exportação ou notificações.
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
