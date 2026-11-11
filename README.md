# SGMA — Gestão de serviços aeronáuticos

Projeto acadêmico de Tópicos em Engenharia de Software / UFSCar. Implementação em Java 17, Spring Boot, Spring Data JPA, Thymeleaf e Gradle.

## Entrega parcial implementada

- Entidades JPA e DAOs do diagrama, com herança `JOINED` para usuários.
- Cadastro de clientes e aeronaves; matrícula única entre aeronaves ativas.
- Abertura e pesquisa de solicitações por cliente, matrícula, descrição e situação, com paginação.
- Ficha da solicitação, início de análise e cancelamento com motivo.
- Complementos: pergunta do gestor, resposta simulada do cliente e retorno automático à análise, com histórico.
- Histórico persistido com autor, data e situações anterior/nova; controle de concorrência otimista na solicitação.
- Painel com contagens reais e interface responsiva em português.
- Testes de integração de persistência, formulários, filtros e transições.

**Escopo:** portal local sem autenticação ou autorização por perfil. O operador local identifica as ações administrativas no histórico; a abertura é atribuída ao cliente da aeronave como parte da simulação. Isso não comprova identidade. O servidor escuta somente em `127.0.0.1` por padrão. Não expor esta versão como serviço público.

Orçamentos possuem envio, revisão com versionamento e aprovação/recusa da versão vigente dentro da validade. Encaminhamento e confirmação de realização também estão disponíveis, concluindo o fluxo. Também ficam para próximas entregas: login/perfis, anexos, edição/inativação dos cadastros e exportação. As senhas de usuários são marcadores sem login, não credenciais utilizáveis.

## Abrir rapidamente sem MySQL

Pré-requisito: **JDK 17** (incluindo `javac`) e acesso à internet na primeira execução. Não é necessário instalar Gradle.

No PowerShell, na pasta do repositório:

```powershell
cd sgma
# Se necessário, ajuste o caminho para sua instalação:
$env:JAVA_HOME = 'C:\caminho\do\jdk-17'
.\gradlew.bat bootRun --args='--spring.profiles.active=demo'
```

Abra **http://localhost:8081**. O perfil `demo` usa H2 em arquivo, em `sgma/data/`, e preserva os dados entre reinicializações. Não utiliza o MySQL. Para encerrar, pressione `Ctrl+C`.

Também está disponível `powershell -ExecutionPolicy Bypass -File .\sgma\iniciar-demo.ps1`, a partir da raiz. O script procura o JDK local em `%LOCALAPPDATA%\sgma-tools` se `JAVA_HOME` estiver vazio; esse JDK não é incluído no Git. A opção `-Porta 8082` permite usar outra porta.

## Executar com MySQL

Instale/inicie o MySQL e configure uma conta com permissão para criar/atualizar as tabelas do banco `sgma`. A URL padrão tenta criar o banco caso ele ainda não exista; se a conta não tiver essa permissão, crie o banco previamente.

Na pasta `sgma`:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-17'
$env:DB_USER = 'seu_usuario'
$env:DB_PASSWORD = 'sua_senha'
# Opcional: outra URL/banco
$env:DB_URL = 'jdbc:mysql://localhost:3306/sgma?createDatabaseIfNotExist=true'
.\gradlew.bat bootRun
```

Não grave senhas no repositório. O esquema usa `ddl-auto=update`, adequado a esta etapa de desenvolvimento. O modo MySQL precisa ser validado com a instalação de cada integrante; os testes automatizados usam H2 em memória.

## Roteiro da apresentação

1. Abra **Clientes** e cadastre um cliente fictício.
2. Em **Aeronaves**, cadastre uma matrícula e selecione o cliente.
3. Em **Solicitações → Nova solicitação**, selecione aeronave, tipo e descrição.
4. Confira a ficha e o evento de abertura no histórico.
5. Clique em **Iniciar análise** e confira o novo evento e os indicadores no painel.
6. Na ficha em análise, preencha **Pergunta ao cliente** e clique em **Solicitar complemento**. Em **Complementos de informação**, envie uma resposta. Confira o retorno para análise e os novos eventos no histórico. É possível repetir o ciclo.
7. Pesquise a solicitação por matrícula e situação.
8. Na ficha, expanda **Cancelar solicitação**, informe o motivo e confirme. O histórico será preservado e novas ações não estarão disponíveis.

O banco inicia sem dados de clientes/aeronaves. O operador e o responsável pela confirmação locais são criados automaticamente, sem duplicação a cada início.

## Testar e empacotar

Na pasta `sgma`, com `JAVA_HOME` apontando para o JDK 17:

```powershell
.\gradlew.bat test bootJar
```

Os testes usam perfil `test` com H2 em memória e não acessam o banco da demonstração. Relatório: `sgma/build/reports/tests/test/index.html`. JAR executável: `sgma/build/libs/sgma-0.0.1-SNAPSHOT.jar`.

## Organização

- `sgma/src/main/java/br/ufscar/tes/sgma/domain`: entidades e enums.
- `dao`: interfaces Spring Data (`CrudRepository`), conforme o tutorial.
- `service`: cadastros, validações e operações da solicitação.
- `controller`: páginas e tratamento dos formulários.
- `sgma/src/main/resources/templates` e `static/css`: interface Thymeleaf.
- `sgma/docs`: diagramas originais de referência. Seus pacotes ainda usam o nome abreviado anterior; o código usa `br.ufscar.tes.sgma.domain`.

O passo 6 do tutorial foi adaptado: em vez de inserir dados de negócio a cada inicialização, a demonstração ocorre pelos formulários e pelos testes. Arquivos de build e bancos locais são ignorados pelo Git.

## Testar orçamentos

Na ficha em análise, preencha descrição, valor e validade em **Orçamentos**. Envie e escolha **Aprovar orçamento**, **Recusar orçamento** ou **Iniciar revisão**. A revisão volta para análise, invalida a versão anterior e permite enviar a próxima versão sem apagar o histórico. Validade inclui o dia informado. Orçamento vencido exige revisão antes de uma decisão. Valores devem ser positivos com até duas casas decimais. A decisão ainda simula o cliente vinculado, sem login. Após aprovação, use o encaminhamento e a confirmação para finalizar.

## Encaminhar e concluir

Em uma solicitação **Aprovada**, descreva o destino/encaminhamento e clique em **Registrar encaminhamento**. A situação muda para **Encaminhada**, sem possibilidade de cancelamento. Informe a data de realização (do dia do encaminhamento até hoje), uma referência opcional de até 500 caracteres e clique em **Confirmar realização**. A situação muda para **Concluída** e preserva o histórico, as versões do orçamento e os complementos. A confirmação é única e atribuída ao responsável local da demonstração, ainda sem autenticação.
