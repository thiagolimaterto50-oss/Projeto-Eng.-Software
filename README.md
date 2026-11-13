SGMA — Gestão de serviços aeronáuticos

Projeto acadêmico de Tópicos em Engenharia de Software / UFSCar. Java 17, Spring Boot, Spring Security, Thymeleaf, Spring Data JPA e MySQL.

Como rodar
1. Instale os pré-requisitos
JDK 17 (por exemplo, Eclipse Temurin: https://adoptium.net)
MySQL 8 (o serviço precisa estar em execução)
Git

Não é necessário instalar o Gradle: o projeto já inclui o gradlew.

2. Configure o MySQL

A aplicação usa o usuário root com a senha root. O banco sgma e as tabelas são criados automaticamente.

Se a senha do seu root for outra, ajuste-a no MySQL:

sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
3. Baixe o projeto
bash
git clone https://github.com/thiagolimaterto50-oss/Projeto-Eng.-Software.git
cd Projeto-Eng.-Software/sgma
4. Execute

Windows (PowerShell):

powershell
.\gradlew.bat bootRun

Linux/macOS:

bash
chmod +x gradlew
./gradlew bootRun

Espere aparecer Started SgmaApplication no terminal. Na primeira vez, o download das dependências pode levar alguns minutos.

5. Acesse

Abra http://localhost:8081 e entre com uma das contas de demonstração:

Perfil	E-mail	Senha
Gestor	operador@sgma.invalid	Gestor123!
Cliente	cliente@sgma.invalid	Cliente123!
Responsável	confirmacao@sgma.invalid	Responsavel123!

Para encerrar a aplicação, use Ctrl+C no terminal.
