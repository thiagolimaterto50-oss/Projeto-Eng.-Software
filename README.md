# SGMA — Gestão de serviços aeronáuticos

Projeto acadêmico de Tópicos em Engenharia de Software da UFSCar. Java 17, Spring Boot, Spring Security, Thymeleaf, Spring Data JPA e MySQL.

## 1. Preparar a máquina

Instale:

- **JDK 17**, incluindo `java` e `javac`. [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17).
- **MySQL Community Server 8.0 ou 8.4**, com o servidor iniciado e uma conta administrativa para preparar o banco. [Download oficial](https://dev.mysql.com/downloads/mysql/8.4.html). O Workbench é um cliente opcional. O serviço MySQL Server precisa estar em execução.
- **Git**, para baixar o repositório.

Confira `java -version`, `javac -version` e `git --version` no terminal. A primeira execução precisa de internet para baixar as dependências. O repositório inclui o Gradle Wrapper.

## 2. Baixar a entrega

```sh
git clone --branch main https://github.com/thiagolimaterto50-oss/Projeto-Eng.-Software.git
cd Projeto-Eng.-Software/sgma
```

Execute os comandos de inicialização dentro da pasta `sgma`, que contém `gradlew` e `gradlew.bat`.

## 3. Preparar o MySQL

Entre no cliente SQL com a senha administrativa que já existe na sua instalação:

```sh
mysql -u root -p
```

Se `mysql` estiver fora do PATH, abra o MySQL Command Line Client instalado com o servidor ou execute o bloco SQL pelo Workbench.

Crie o banco e uma conta exclusiva do projeto:

```sql
CREATE DATABASE IF NOT EXISTS sgma CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'sgma'@'localhost' IDENTIFIED BY 'SgmaLocal123!';
GRANT ALL PRIVILEGES ON sgma.* TO 'sgma'@'localhost';
EXIT;
```

`SgmaLocal123!` é uma senha de exemplo para a avaliação local. Se escolher outra senha, informe o mesmo valor em `DB_PASSWORD`. Se o usuário `sgma` já existir, utilize sua senha atual. `CREATE USER IF NOT EXISTS` preserva a senha existente.

As permissões são restritas ao banco `sgma`. O Hibernate cria e atualiza as tabelas na inicialização. Os dados permanecem no servidor MySQL ao encerrar a aplicação.

## 4. Configurar a conexão e iniciar

### Windows — PowerShell

Dentro da pasta `sgma`:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/sgma'
$env:DB_USER = 'sgma'
$env:DB_PASSWORD = 'SgmaLocal123!'
.\gradlew.bat bootRun
```

### Linux/macOS — terminal

Dentro da pasta `sgma`:

```sh
export DB_URL='jdbc:mysql://localhost:3306/sgma'
export DB_USER='sgma'
export DB_PASSWORD='SgmaLocal123!'
chmod +x gradlew
./gradlew bootRun
```

Ajuste host e porta em `DB_URL` conforme a instalação do MySQL. Defina as variáveis no mesmo terminal em que executar o projeto, inclusive ao iniciar novamente.

Espere aparecer `Started SgmaApplication`. Abra **http://localhost:8081**. O servidor web aceita conexões locais por padrão. Use **Ctrl+C** para encerrar a aplicação.

## 5. Acessar e verificar o fluxo

A primeira inicialização local cria as contas abaixo e a aeronave `PT-LOGIN`, vinculada ao cliente de demonstração. Inicializações posteriores preservam as contas e suas senhas.

| Perfil | E-mail | Senha inicial |
|---|---|---|
| Cliente | cliente@sgma.invalid | Cliente123! |
| Gestor | operador@sgma.invalid | Gestor123! |
| Responsável pela confirmação | confirmacao@sgma.invalid | Responsavel123! |

1. Entre como **cliente**, abra **Solicitações**, clique em **Nova solicitação**, escolha a aeronave `PT-LOGIN`, o tipo e uma descrição. Anote o número SG do pedido.
2. Use **Sair** e entre como **gestor**. Abra o mesmo pedido, inicie a análise e envie um orçamento com descrição, valor positivo e validade futura.
3. Saia e entre como **cliente**. Aprove a versão vigente. A situação passa a **Aprovada**.
4. Como **gestor**, registre o encaminhamento com uma descrição. A situação passa a **Encaminhada**.
5. Como **responsável**, informe a data de realização de hoje e uma referência opcional. Confirme a realização. A situação passa a **Concluída**.
6. Confira o histórico com os usuários de cada ação. Encerre a aplicação, inicie novamente com as mesmas variáveis e consulte o mesmo número SG. O registro deve continuar disponível.

O cadastro público cria contas de cliente. Para uma nova conta, o gestor pode cadastrar uma aeronave vinculada a ela em **Aeronaves**. **Minha conta** permite alterar a própria senha.

## 6. Senhas e persistência

O Spring Security autentica as contas do banco e compara a senha informada com um **hash BCrypt**. Cadastro, alteração de senha e contas iniciais usam `PasswordEncoder`. As operações verificam o perfil e a propriedade dos registros na camada de serviço. Os formulários Thymeleaf incluem proteção CSRF.

A aplicação utiliza **MySQL**. H2 é uma dependência exclusiva dos testes automatizados. O navegador nesta execução local usa HTTP. Para disponibilizar o serviço em rede, configure HTTPS.

As contas conhecidas acima são criadas nos ambientes locais, quando o perfil `prod` está desativado. Em `prod`, configure as contas iniciais por `SGMA_GESTOR_EMAIL`, `SGMA_GESTOR_SENHA`, `SGMA_RESPONSAVEL_EMAIL` e `SGMA_RESPONSAVEL_SENHA`. Senhas já configuradas são preservadas.

## 7. Testes e pacote executável

Dentro de `sgma`:

```powershell
# Windows
.\gradlew.bat test bootJar
```

```sh
# Linux/macOS
./gradlew test bootJar
```

Os testes usam H2 em memória com o perfil `test`. Cobrem login, BCrypt, CSRF, permissões, cadastros, anexos e o fluxo de solicitações. Relatório em `build/reports/tests/test/index.html`. Para exercitar o MySQL, siga o fluxo e a reinicialização da seção 5.

Após gerar o pacote, também é possível iniciar com as mesmas variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`:

```sh
java -jar build/libs/sgma-0.0.1-SNAPSHOT.jar
```

## Problemas ao iniciar

| Sintoma | O que conferir |
|---|---|
| `JAVA_HOME` inválido ou compilador indisponível | Use a pasta de um JDK 17. Confira `java -version` e `javac -version`. |
| `Communications link failure` ou conexão recusada | Inicie o MySQL Server. Confira host e porta em `DB_URL`. |
| `Access denied for user` | Confira usuário, senha, permissões e as variáveis do terminal que inicia a aplicação. |
| `Unknown database 'sgma'` | Execute o `CREATE DATABASE` da seção 3. |
| Porta 8081 ocupada | Encerre a instância anterior ou use `$env:PORT='8082'` no PowerShell / `export PORT=8082` no Linux/macOS e abra a nova porta. |
| Login inicial falha após mudar a senha | Entre com a senha atual. A inicialização preserva senhas existentes. |
| Gradle não consegue baixar dependências | Confira internet e configurações de proxy da rede. |

A [documentação da entrega](sgma/docs/entrega-parcial.md) descreve funcionalidades e verificações. Os diagramas estão em `sgma/docs`.

## Status dos requisitos

Conferência individual em **15/11/2026**, sobre a implementação do commit [`e3c5a84`](https://github.com/thiagolimaterto50-oss/Projeto-Eng.-Software/tree/e3c5a84f6670a560baaf16b068c8cc50aab154b3). Base documental fornecida pela equipe: **Documento de Requisitos do SGMA Simplificado, versão 1.0, de 25/09/2026** (`Documento_de_Requisitos_SGMA_Simplificado_Final.pdf`). A especificação se identifica como proposta com validação da equipe/stakeholders pendente.

**Implementado** indica comportamento encontrado no código, com evidências citadas na linha. **Parcial** indica funcionalidade existente com lacunas frente ao texto do requisito. **Pendente** indica implementação ou ensaio ausente. **A validar** indica evidência insuficiente para afirmar atendimento integral. **Verificado** é usado para os cenários de aceitação e o caso de uso, conforme os testes e inspeções descritos.

Revisão complementar em **16/11/2026** para RF001, RF002, RF003, RF014 e RN01, incluindo regressão do isolamento entre clientes. A última execução da suíte teve **22 testes aprovados**. A verificação com MySQL e navegador está registrada na [documentação da entrega][verif]. As tabelas distinguem evidência automatizada, inspeção do código e ensaio manual. O atendimento de um critério de aceitação não implica que todos os requisitos relacionados estejam completos.

### Requisitos funcionais

| ID | Requisito resumido | Status | Evidência e pendência |
|---|---|---|---|
| RF001 | Autenticar usuários e autorizar operações por perfil | Implementado | Login, BCrypt, permissões de autoatendimento e bloqueios por perfil. Sessão vinculada ao ID imutável do usuário, preservando a identidade após alteração/reuso de e-mail. [Segurança][seg], [acesso][acesso] e testes de perfis/sessão em [integração][tests]. |
| RF002 | Cliente cadastrar e atualizar identificação e contato | Implementado | Cadastro público e edição de nome, e-mail, documento e telefone em Minha conta. Identidade vem da sessão; valida duplicidade e impede alterar perfil/ativação ou outro cliente. [Cadastro][cad], [conta][conta], [cadastro público][publicTests] e testes `autoEdicao*` em [integração][tests]. |
| RF003 | Cliente cadastrar aeronave; gestor corrigir vínculo com justificativa | Implementado | Cliente cadastra na própria conta. Somente gestor corrige o proprietário, com justificativa obrigatória e histórico de anterior/novo cliente, autor e instante. Pedidos antigos preservam seu cliente original. [Cadastro][cad], [aeronave][aero] e testes `clienteCadastraAeronaveSomenteNaPropriaConta` / `correcaoDeVinculoAuditadaPreservaPedidosAntigos` em [integração][tests]. |
| RF004 | Abrir pedido para aeronave própria, com tipo, descrição e anexo opcional | Implementado | `abrir` verifica cliente e propriedade. A tela aceita anexo, armazenado no banco e com download autorizado. [Serviço][serv], [anexos][anexos] e testes de isolamento/anexos em [integração][tests]. |
| RF005 | Gerar identificador, instante de abertura e estado Aberta | Implementado | ID gerado pelo JPA. O construtor registra data/hora, estado e evento de abertura. [Entidade][ent], [solicitação][sol] e teste do fluxo completo em [integração][tests]. |
| RF006 | Gestor iniciar análise e registrar observações de triagem | Implementado | `iniciarAnalise` exige Aberta; `registrarTriagem` exige Em análise e gestor autenticado. [Serviço][serv], [solicitação][sol] e teste do fluxo completo em [integração][tests]. |
| RF007 | Gestor solicitar complemento e registrar pergunta | Implementado | Pergunta persistida e mudança para Aguardando informação. Impede uma segunda pergunta enquanto há resposta pendente. [Solicitação][sol] e testes de complementos em [integração][tests]. |
| RF008 | Cliente responder complemento e retornar à análise | Implementado | Verifica autor, vínculo e estado; persiste resposta e evento de retorno. [Serviço][serv], [solicitação][sol] e testes de complementos em [integração][tests]. |
| RF009 | Orçamento com descrição, valor, validade e versão | Implementado | Campos obrigatórios, valor positivo com duas casas e versão incremental. [Orçamento][orc], [serviço][serv] e testes de orçamento em [integração][tests]. |
| RF010 | Enviar orçamento e preservar versões, com decisão somente da vigente | Implementado | Revisão mantém a versão antiga e a torna indisponível para decisão. Nova versão exige novo envio. [Solicitação][sol], [ficha][ficha] e teste do fluxo completo em [integração][tests]. |
| RF011 | Cliente aprovar/recusar versão vigente com autor e instante | Implementado | O serviço usa o cliente autenticado; o orçamento registra decisão, decisor e data/hora. [Serviço][serv], [orçamento][orc] e testes do fluxo e recusa em [integração][tests]. |
| RF012 | Gestor encaminhar pedido aprovado | Implementado | Exige gestor e estado Aprovada; registra descrição, instante e histórico. [Serviço][serv], [solicitação][sol] e testes do fluxo/perfis em [integração][tests]. |
| RF013 | Responsável confirmar realização de pedido encaminhado | Implementado | Registra data informada, responsável autenticado, instante e referência opcional. [Confirmação][conf], [serviço][serv] e teste `finalizacaoValidaDatasEBloqueiaRepeticaoECancelamento` em [integração][tests]. |
| RF014 | Consultar pedidos, estados e orçamentos conforme perfil | Implementado | Cliente vê seus registros, inclusive busca e contagens. Perfis internos consultam fichas e orçamentos. [Controller][ctrl], [acesso][acesso] e teste `clienteEnxergaSomenteSeusRegistrosInclusiveContagens` em [integração][tests]. |
| RF015 | Histórico consultável com versões, decisões, confirmação, autor e instante | Implementado | Eventos persistidos e ordenados, com versões mantidas na ficha. MySQL preservou histórico após reinício. [Solicitação][sol], [ficha][ficha] e [verificação da entrega][verif]. |

### Requisitos não funcionais

| ID | Requisito resumido | Status | Evidência e pendência |
|---|---|---|---|
| RNF001 | Português do Brasil e ações essenciais em 360 px e 1280 px | A validar | Templates em português e CSS responsivo. A medição nessas duas larguras foi feita no protótipo anterior ao login; falta repetir o conjunto de telas da versão atual. [CSS][css], [templates][templates] e [verificação registrada][verif]. |
| RNF002 | Negar acesso indevido também na camada de processamento | Implementado | Serviços consultam usuário autenticado e verificam perfil/propriedade. Há teste de chamada direta ao serviço e tentativa HTTP de outro cliente. [Acesso][acesso], [serviço][serv] e testes de permissões em [integração][tests]. |
| RNF003 | Conexão criptografada e senhas armazenadas de forma protegida | Parcial | BCrypt confirmado nos cadastros, alterações e quatro contas do ensaio MySQL. A execução documentada usa HTTP; falta configurar e verificar HTTPS. [Segurança][seg], [configuração][props] e [verificação][verif]. |
| RNF004 | 95% das consultas em até 3 s com 50 sessões simultâneas | Pendente | Falta definir/validar o ambiente e executar o ensaio de carga. O documento trata a carga como meta acadêmica proposta. A suíte funcional de [testes][tests] não mede esse critério. |
| RNF005 | Backup a cada 24 h e teste documentado de restauração | Pendente | Não há rotina agendada nem evidência de restauração no repositório. O ensaio de reinício descrito na [verificação][verif] demonstra persistência; restauração de backup continua pendente. |
| RNF006 | Erros identificarem campo e correção; falha ao salvar não indicar sucesso | Implementado | Verificado em 21/11/2026 com Gradle, MockMvc, Thymeleaf e H2 (23 testes aprovados). O [catálogo de mensagens](sgma/src/main/resources/messages.properties) orienta por campo. Os [testes de integração][tests] verificam o HTML, os valores preservados, as mensagens e a persistência em envios inválidos e válidos de orçamento e confirmação. |

### Regras de negócio

| ID | Requisito resumido | Status | Evidência e pendência |
|---|---|---|---|
| RN01 | Cliente acessar somente seus pedidos e aeronaves; perfis internos respeitarem atribuições | Implementado | Corrigida a identificação de cliente carregado como proxy JPA, que podia retirar o filtro. Perfil desconhecido é negado. Testes cobrem proxy, sessão após troca/reuso do e-mail, URLs, anexos e mudança do proprietário. [Acesso][acesso] e [integração][tests]. |
| RN02 | Matrícula única entre aeronaves ativas e pedido vinculado a aeronave cadastrada | Implementado | `matriculaAtiva` é única no banco e fica nula para inativas. A abertura busca uma aeronave existente e ativa. [Aeronave][aero], [serviço][serv] e testes de cadastro/inativação em [integração][tests]. |
| RN03 | Aprovar somente versão vigente e válida; vencida exige nova versão | Implementado | Validade e vigência são verificadas ao decidir. Revisão permite novo envio e a ficha orienta o usuário após vencimento. [Orçamento][orc], [ficha][ficha] e teste `orcamentoVencidoValoresInvalidosERecusaFinal` em [integração][tests]. |
| RN04 | Revisão exigir nova decisão, sem herdar aprovação | Implementado | Cada nova versão começa PENDENTE. A revisão segue a tabela de transições, a partir de Orçamento enviado. [Orçamento][orc], [solicitação][sol] e teste do fluxo com duas versões em [integração][tests]. |
| RN05 | Aprovada para Encaminhada; Encaminhada para Concluída pelo responsável | Implementado | Guardas de estado e perfil no domínio/serviço; conclusão antecipada é rejeitada. [Solicitação][sol], [serviço][serv] e testes de finalização/perfis em [integração][tests]. |
| RN06 | Confirmação com finalidade administrativa | Implementado | O registro contém realização informada e referência. O fluxo termina na confirmação administrativa; emissão de liberação técnica fica fora do escopo implementado. [Confirmação][conf] e [ficha][ficha], conferidas por inspeção. |
| RN07 | Cancelamento com motivo antes do encaminhamento e estados finais sem reabertura | Implementado | `isCancelavel` enumera os cinco estados permitidos. `cancelar` exige motivo e cliente dono/gestor. Métodos de transição bloqueiam saídas dos estados finais. [Solicitação][sol] e testes de recusa/finalização em [integração][tests]. |

### Critérios de aceitação

| ID | Requisito resumido | Status | Evidência e pendência |
|---|---|---|---|
| CA01 | Cliente B não abrir nem consultar pedido da aeronave do cliente A | Verificado | Teste `clienteEnxergaSomenteSeusRegistrosInclusiveContagens` cobre abertura e consulta indevidas. Ensaio MySQL confirmou HTTP 403 para ficha/anexo de outro cliente. [Testes][tests] e [verificação][verif]. |
| CA02 | Bloquear aprovação vencida e orientar nova versão | Verificado | Teste força vencimento e verifica rejeição. A ficha exibe orientação de revisão. [Teste de orçamento vencido][tests] e [ficha][ficha]. |
| CA03 | Versão revisada exigir decisão própria | Verificado | Inspeção confirma decisão inicial PENDENTE. Teste revisa o orçamento, rejeita decisão na versão anterior e aprova explicitamente a nova. Revisão após aprovação não é uma transição prevista na seção 4. [Orçamento][orc], [solicitação][sol] e [teste do fluxo][tests]. |
| CA04 | Impedir conclusão de pedido apenas aprovado | Verificado | O responsável tenta confirmar antes do encaminhamento e recebe erro no teste de finalização. [Teste `finalizacaoValidaDatasEBloqueiaRepeticaoECancelamento`][tests]. |
| CA05 | Após confirmação, exibir autor, instante e Concluída, sem liberação técnica | Verificado | Fluxo autenticado e consulta do histórico conferidos no MySQL. Inspeção do modelo/tela confirma conteúdo administrativo. [Confirmação][conf], [ficha][ficha] e [verificação][verif]. |

### Caso de uso

| ID | Requisito resumido | Status | Evidência e pendência |
|---|---|---|---|
| UC01 | Solicitar orçamento e acompanhar manutenção | Verificado | Fluxo principal autenticado exercitado com MySQL. A suíte cobre triagem, complemento, revisão, recusa e cancelamento. Em 16/11, também foi exercitado no navegador o cadastro público, cadastro da própria aeronave e abertura do pedido. [Teste do fluxo completo][tests] e [verificação][verif]. |

As decisões em aberto da seção 8 incluem criação de usuários, correção do vínculo cliente–aeronave, comprovante da confirmação, cancelamento após aprovação, validade padrão, limites de anexos, retenção e carga de desempenho. A implementação atual adota algumas dessas escolhas, como anexo de até 5 MB, cancelamento antes do encaminhamento e referência opcional. A aprovação dessas escolhas pela equipe e pelos interessados continua pendente de registro.

[seg]: sgma/src/main/java/br/ufscar/tes/sgma/config/WebSecurityConfig.java
[acesso]: sgma/src/main/java/br/ufscar/tes/sgma/service/AcessoService.java
[cad]: sgma/src/main/java/br/ufscar/tes/sgma/service/CadastroService.java
[conta]: sgma/src/main/java/br/ufscar/tes/sgma/controller/ContaController.java
[publicTests]: sgma/src/test/java/br/ufscar/tes/sgma/CadastroPublicoTests.java
[tests]: sgma/src/test/java/br/ufscar/tes/sgma/PortalIntegrationTests.java
[serv]: sgma/src/main/java/br/ufscar/tes/sgma/service/SolicitacaoService.java
[anexos]: sgma/src/main/java/br/ufscar/tes/sgma/service/AnexoService.java
[ent]: sgma/src/main/java/br/ufscar/tes/sgma/domain/Entidade.java
[sol]: sgma/src/main/java/br/ufscar/tes/sgma/domain/Solicitacao.java
[orc]: sgma/src/main/java/br/ufscar/tes/sgma/domain/Orcamento.java
[conf]: sgma/src/main/java/br/ufscar/tes/sgma/domain/Confirmacao.java
[aero]: sgma/src/main/java/br/ufscar/tes/sgma/domain/Aeronave.java
[ctrl]: sgma/src/main/java/br/ufscar/tes/sgma/controller/PortalController.java
[ficha]: sgma/src/main/resources/templates/detalhes.html
[frag]: sgma/src/main/resources/templates/fragmentos.html
[erros]: sgma/src/main/java/br/ufscar/tes/sgma/controller/ErroControllerAdvice.java
[verif]: sgma/docs/entrega-parcial.md
[css]: sgma/src/main/resources/static/css/portal.css
[templates]: sgma/src/main/resources/templates
[props]: sgma/src/main/resources/application.properties
