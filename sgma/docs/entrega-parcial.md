# Entrega parcial — 10/11/2026

## Implementado e verificado

- Entidades e DAOs de todo o diagrama, incluindo relacionamentos e herança JOINED.
- Portal com painel, cadastro de clientes e aeronaves, abertura, pesquisa e detalhes de solicitações.
- Transições implementadas: ABERTA → EM_ANALISE e cancelamento das situações anteriores ao encaminhamento.
- Motivo obrigatório no cancelamento; histórico com autor, instante e transição; rejeição de reabertura de solicitação cancelada.
- Matrícula normalizada em maiúsculas e chave auxiliar única para aeronaves ativas. Esta chave permite manter registros inativos com a mesma matrícula quando a inativação for implementada.
- Formulários com validação e CSRF; solicitações com controle otimista de concorrência.

## Evidência de validação

- `gradlew test bootJar`: passou, quatro testes automatizados sem falhas (um de contexto e três de integração).
- Integração: criação e leitura de entidades, herança/relacionamentos, validação de entrada, matrícula duplicada, filtros, cancelamento, histórico e rejeição de POST sem CSRF.
- Navegador Microsoft Edge via Playwright: cadastro de cliente e aeronave, abertura, análise, cancelamento, leitura do histórico após recarregar e filtro por matrícula/situação. Sem erros JavaScript.
- Inspeção visual em 1440 px e 390 px. A página não transborda horizontalmente; tabelas possuem rolagem própria.
- Execução real com JDK 17 e banco H2 em arquivo. Cliente fictício preservado após reinicialização da aplicação.
- Capturas locais em `build/verification/` (não versionadas).

## Limites da entrega

- MySQL configurado, mas ainda não validado nesta máquina, que não possui serviço MySQL disponível.
- Não há login/perfis: é uma demonstração local. O autor da abertura é o cliente vinculado à aeronave; as ações administrativas usam o operador local. Atribuição de histórico não representa autenticação.
- Encaminhamento e confirmação implementados. Orçamentos agora têm envio, revisão e decisão. Os testes de persistência dessas entidades não demonstram o ciclo de negócio completo.
- Sem anexos, edição/inativação de cadastros, exportação ou notificações.

## Próximas etapas sugeridas

1. Validar o banco MySQL usado pelo grupo e confirmar o critério da entrega com o professor.
2. Validar os orçamentos com o grupo; envio, versões, validade e decisão implementados.
3. Revisar com o grupo o fluxo completo, incluindo encaminhamento e confirmação já implementados.
4. Adicionar autenticação e autorização por perfil e propriedade dos registros.

Os arquivos gerados de `build/` foram retirados do índice do Git e continuam disponíveis localmente. O `.gitignore` evita versionar builds, bancos locais e arquivos de IDE.

## Incremento: complementos

Perguntas e respostas disponíveis na ficha, com transições EM_ANALISE → AGUARDANDO_INFORMACAO → EM_ANALISE, datas e autoria no histórico. Apenas uma pergunta pendente por solicitação. Respostas repetidas, de outra solicitação ou posteriores ao cancelamento são rejeitadas. O modo local simula o cliente vinculado; autenticação permanece pendente.

Dois testes de integração adicionais cobrem o ciclo, múltiplas rodadas, histórico/autoria, campos vazios ou longos, CSRF, duplicidade e vínculos incorretos.

## Incremento: orçamentos

Envio durante análise, revisão preservando versões e decisão da versão vigente dentro da validade. Aprovação leva a APROVADA e recusa a RECUSADA. Validação de valor positivo, precisão monetária e validade. Histórico com autor, versão e transição. Dois testes adicionais cobrem envio, revisão, aprovação, recusa, vencimento, versões antigas e entrada inválida.

## Incremento: encaminhamento e confirmação

APROVADA → ENCAMINHADA exige descrição; ENCAMINHADA → CONCLUIDA exige data de realização e aceita referência opcional. Data deve estar entre encaminhamento e hoje. Histórico com autor e instante em cada etapa. Cancelamento bloqueado após encaminhamento; confirmação duplicada e reabertura rejeitadas. Responsável local criado automaticamente para a demonstração sem login. Dois testes adicionais cobrem o ciclo final, persistência, histórico, estados incorretos e validação de datas.
