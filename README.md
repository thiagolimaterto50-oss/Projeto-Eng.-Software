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
