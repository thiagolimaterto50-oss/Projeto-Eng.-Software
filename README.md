# SGMA — Gestão de serviços aeronáuticos

Protótipo acadêmico de Tópicos em Engenharia de Software / UFSCar. Java 17, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA e Gradle.

## Abrir neste computador

Na raiz do repositório, execute no PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\sgma\iniciar-demo.ps1
```

Espere `Started SgmaApplication` e abra **http://localhost:8081**. Mantenha o terminal aberto; `Ctrl+C` encerra a aplicação. O script procura um JDK 17 em `JAVA_HOME` ou em `%LOCALAPPDATA%\sgma-tools`. Para outra porta, acrescente `-Porta 8082`.

Em outro computador, instale o JDK 17 e execute:

```powershell
cd sgma
$env:JAVA_HOME = 'C:\caminho\do\jdk-17'
.\gradlew.bat bootRun --args='--spring.profiles.active=demo'
```

A primeira execução precisa de internet para baixar dependências. Não é necessário instalar Gradle. O perfil `demo` usa H2 em arquivo (`sgma/data/`) e preserva dados entre reinicializações.

## Contas de demonstração

Disponíveis **somente nos perfis demo e test**, criadas na primeira inicialização:

| Perfil | E-mail | Senha inicial |
|---|---|---|
| Gestor | operador@sgma.invalid | Gestor123! |
| Cliente | cliente@sgma.invalid | Cliente123! |
| Responsável pela confirmação | confirmacao@sgma.invalid | Responsavel123! |

A aeronave `PT-LOGIN` pertence ao cliente de demonstração. São dados fictícios. Não use as contas públicas de demonstração em uma instalação de produção.

A senha pode ser alterada em **Minha conta**. Reiniciar não redefine senhas ou reativa contas. Contas antigas com o marcador `!SEM_LOGIN` recebem senha somente quando correspondem às contas iniciais configuradas. Para habilitar os demais clientes antigos, o gestor deve abrir **Clientes → Editar** e definir uma senha.

## Permissões

| Operação | Cliente | Gestor | Responsável |
|---|---|---|---|
| Consultar painel, aeronaves, solicitações e anexos | Apenas seus registros | Todos | Todos |
| Criar/editar/inativar clientes e aeronaves | — | Sim | — |
| Abrir solicitação | Própria aeronave ativa | — | — |
| Analisar, registrar triagem e pedir complemento | — | Sim | — |
| Responder complemento | Própria solicitação | — | — |
| Enviar/revisar orçamento | — | Sim | — |
| Aprovar/recusar orçamento | Própria solicitação | — | — |
| Cancelar antes do encaminhamento | Própria solicitação | Sim | — |
| Anexar/substituir arquivo antes do encaminhamento ou encerramento | Própria solicitação | Sim | — |
| Encaminhar serviço aprovado | — | Sim | — |
| Confirmar realização | — | — | Sim |

As verificações acontecem no servidor, inclusive por URL direta. Clientes inativos são impedidos de entrar e de continuar usando sessões abertas. Senhas são armazenadas com BCrypt e os formulários possuem proteção CSRF. Observações de triagem são internas e não aparecem para o cliente.

## Funcionalidades

- Entidades JPA e DAOs, com herança `JOINED` para usuários.
- Cadastro e edição de clientes, senha inicial/redefinição pelo gestor e inativação/reativação.
- Cadastro e edição de aeronaves, justificativa opcional do vínculo, inativação/reativação e matrícula única entre aeronaves ativas.
- O proprietário da aeronave é preservado na edição para manter consistência com o histórico. Transferência de proprietário não faz parte desta entrega.
- Abertura, pesquisa com filtros e paginação, análise, triagem e cancelamento justificado.
- Perguntas/respostas de complementação, permitindo novas rodadas após a resposta.
- Orçamentos com valor positivo, validade inclusiva, versões preservadas, revisão e decisão da versão vigente.
- Encaminhamento e confirmação única, com realização entre a data do encaminhamento e hoje.
- Um anexo PDF, PNG ou JPEG de até 5 MB por solicitação. Enviar outro substitui o arquivo atual. Dados armazenados no banco, download autenticado como arquivo, validação de extensão e assinatura inicial. Não há antivírus integrado.
- Histórico persistido de mudanças com autor autenticado e data. Registros antigos gerados na fase de simulação são preservados.
- Painel com contagens conforme o perfil e interface responsiva.

## Roteiro de apresentação

Use navegadores diferentes ou contextos separados para os três perfis. Também é possível usar **Sair** e entrar com outro usuário a cada etapa.

1. **Gestor:** cadastre um cliente com senha inicial e uma aeronave vinculada. Ou use o cliente e a aeronave de demonstração.
2. **Cliente:** abra uma solicitação e envie um anexo opcional pela ficha.
3. **Gestor:** inicie a análise, registre a triagem e solicite um complemento.
4. **Cliente:** responda. A solicitação volta para análise.
5. **Gestor:** envie um orçamento. Opcionalmente, inicie revisão e envie outra versão.
6. **Cliente:** aprove o orçamento vigente dentro da validade (ou recuse para demonstrar o encerramento por recusa).
7. **Gestor:** registre o encaminhamento. Cancelamento deixa de ser permitido.
8. **Responsável:** informe a data da realização e uma referência opcional. Confirme.
9. Confira a situação **Concluída**, os detalhes e o histórico com os autores reais.
10. Demonstre o isolamento: o cliente não acessa solicitações ou anexos de outro cliente. Inative uma aeronave e verifique que ela não aparece para novas solicitações.

## MySQL e contas iniciais fora do modo demo

O MySQL continua sendo o banco padrão. Instale/inicie o serviço e configure uma conta com permissão para criar/atualizar tabelas do banco `sgma`. A URL padrão tenta criar o banco se ele ainda não existir.

Na pasta `sgma`, configure variáveis no terminal e execute sem o perfil `demo`:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-17'
$env:DB_USER = 'seu_usuario'
$env:DB_PASSWORD = 'sua_senha'
$env:DB_URL = 'jdbc:mysql://localhost:3306/sgma?createDatabaseIfNotExist=true'
$env:SGMA_GESTOR_EMAIL = 'gestor@seu-dominio.com'
$env:SGMA_GESTOR_SENHA = 'defina-uma-senha-propria'
$env:SGMA_RESPONSAVEL_EMAIL = 'responsavel@seu-dominio.com'
$env:SGMA_RESPONSAVEL_SENHA = 'defina-outra-senha-propria'
.\gradlew.bat bootRun
```

O bootstrap cria essas contas se não existirem; não troca senhas já configuradas. Sem essas variáveis, nenhuma conta padrão é criada fora de `demo`/`test`. O gestor cadastra os clientes pela interface. Senhas precisam de pelo menos 8 caracteres e no máximo 72 bytes UTF-8. Não grave senhas reais no Git.

O servidor escuta em `127.0.0.1` por padrão. `SERVER_ADDRESS` e `PORT` permitem ajustar o endereço e a porta para o ambiente de execução. Banco e implantação precisam de validação própria; esta implementação foi testada com H2. Não houve deploy. O esquema usa `ddl-auto=update`, próprio desta etapa de desenvolvimento.

## Testes e pacote

Na pasta `sgma`, com o JDK configurado:

```powershell
.\gradlew.bat test bootJar
```

São **14 testes** (13 de integração e um de contexto), com H2 em memória. Cobrem login/logout, inativação, mudança de senha, isolamento por cliente, permissões por ação, cadastros, anexos, validações e o fluxo completo. Não acessam os dados locais da demonstração.

Relatório: `sgma/build/reports/tests/test/index.html`. JAR: `sgma/build/libs/sgma-0.0.1-SNAPSHOT.jar`.

## Organização e limites

- `domain`: entidades e operações de domínio; `dao`: interfaces Spring Data.
- `service`: regras, autorização, contas e anexos.
- `controller`: páginas, formulários e downloads; `config`: segurança e contas iniciais.
- `resources/templates` e `static/css`: interface Thymeleaf.
- `sgma/docs`: diagramas originais e estado da entrega. Os diagramas originais ainda não refletem a entidade técnica `Anexo` nem os nomes completos de pacotes.

Fora do escopo implementado: recuperação de senha por e-mail, múltiplos anexos/histórico de arquivos, transferência de proprietário, relatórios/exportação e notificações. Não há tela para criar outros gestores/responsáveis; as contas iniciais são configuradas pelas variáveis acima. Os critérios finais devem ser conferidos com o documento completo de requisitos do grupo.
