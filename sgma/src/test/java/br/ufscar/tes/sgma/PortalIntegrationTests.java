package br.ufscar.tes.sgma;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import br.ufscar.tes.sgma.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired CadastroService cadastro;
  @Autowired SolicitacaoService servico;
  @Autowired ISolicitacaoDAO solicitacoes;
  @Autowired IOrcamentoDAO orcamentos;
  @Autowired IComplementoDAO complementos;
  @Autowired IConfirmacaoDAO confirmacoes;
  @Autowired IResponsavelConfirmacaoDAO responsaveis;
  @Autowired IGestorSolicitacoesDAO gestores;
  @Autowired PlatformTransactionManager transactions;

  private Aeronave aeronave() {
    String chave = UUID.randomUUID().toString().substring(0, 8);
    Cliente cliente =
        cadastro.cadastrarCliente(
            new CadastroService.ClienteForm(
                "Cliente " + chave, chave + "@exemplo.com", chave, "11999999999"));
    return cadastro.cadastrarAeronave(
        new CadastroService.AeronaveForm(
            "PT-" + chave, "Embraer", "Modelo de teste", cliente.getId()));
  }

  private Long complementoPendente(Long id) {
    return new TransactionTemplate(transactions).execute(tx -> solicitacoes.findById(id)
        .orElseThrow().getComplementos().stream().filter(c -> c.getResposta() == null)
        .findFirst().orElseThrow().getId());
  }

  private Long orcamentoAtual(Long id) {
    return new TransactionTemplate(transactions).execute(tx -> solicitacoes.findById(id).orElseThrow().getOrcamentoVigente().getId());
  }

  @Test
  void orcamentosVersionadosRevisaoEAprovacaoMantemHistorico() throws Exception {
    Long id = servico.abrir(new SolicitacaoService.AberturaForm(aeronave().getId(), TipoServico.REPARO, "Reparo")).getId();
    servico.iniciarAnalise(id);
    String url = "/solicitacoes/" + id;
    mvc.perform(post(url + "/orcamentos").with(csrf()).param("descricaoServico", "Reparo inicial")
        .param("valorTotal", "1500.50").param("dataValidade", LocalDate.now().toString()))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    Long antigo = orcamentoAtual(id);
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("1.500,50")));
    mvc.perform(post(url + "/orcamentos/" + antigo + "/revisar").with(csrf()))
        .andExpect(flash().attributeExists("sucesso"));
    assertFalse(orcamentos.findById(antigo).orElseThrow().isVigente());
    servico.enviarOrcamento(id, new SolicitacaoService.OrcamentoForm("Reparo revisado", new BigDecimal("1200.00"), LocalDate.now().plusDays(2)));
    Long novo = orcamentoAtual(id);
    assertThrows(IllegalArgumentException.class, () -> servico.decidirOrcamento(id, antigo, new SolicitacaoService.DecisaoForm(DecisaoOrcamento.APROVADO)));
    mvc.perform(post(url + "/orcamentos/" + novo + "/decidir").with(csrf()).param("decisao", "APROVADO"))
        .andExpect(flash().attributeExists("sucesso"));
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("Reparo revisado")));
    new TransactionTemplate(transactions).executeWithoutResult(tx -> {
      var s = solicitacoes.findById(id).orElseThrow();
      assertEquals(SituacaoSolicitacao.APROVADA, s.getSituacao());
      assertEquals(2, s.getOrcamentos().size());
      assertEquals(2, s.getOrcamentoVigente().getVersao());
      assertEquals(s.getCliente().getId(), s.getOrcamentoVigente().getDecisor().getId());
      assertEquals(6, s.getHistorico().size());
    });
    assertThrows(IllegalStateException.class, () -> servico.iniciarRevisao(id, novo));
    assertThrows(IllegalStateException.class, () -> servico.decidirOrcamento(id, novo, new SolicitacaoService.DecisaoForm(DecisaoOrcamento.RECUSADO)));
  }

  @Test
  void orcamentosRecusamDadosInvalidosVencimentoEVinculosIncorretos() throws Exception {
    var form = new SolicitacaoService.OrcamentoForm("Serviço", new BigDecimal("50.00"), LocalDate.now());
    Long id = servico.abrir(new SolicitacaoService.AberturaForm(aeronave().getId(), TipoServico.OUTRO, "Teste")).getId();
    assertThrows(IllegalStateException.class, () -> servico.enviarOrcamento(id, form));
    servico.iniciarAnalise(id);
    String url = "/solicitacoes/" + id;
    for (String valor : new String[]{"-1", "0", "1.234", "10000000000", "abc"}) {
      mvc.perform(post(url + "/orcamentos").with(csrf()).param("descricaoServico", "Serviço")
          .param("valorTotal", valor).param("dataValidade", LocalDate.now().toString()))
          .andExpect(flash().attributeExists("erro"));
    }
    mvc.perform(post(url + "/orcamentos").with(csrf()).param("descricaoServico", "Serviço")
        .param("valorTotal", "50").param("dataValidade", LocalDate.now().minusDays(1).toString()))
        .andExpect(flash().attributeExists("erro"));
    servico.enviarOrcamento(id, form);
    Long oid = orcamentoAtual(id);
    assertThrows(IllegalArgumentException.class, () -> servico.decidirOrcamento(id, oid + 9999, new SolicitacaoService.DecisaoForm(DecisaoOrcamento.APROVADO)));
    assertThrows(IllegalArgumentException.class, () -> servico.decidirOrcamento(id, oid, new SolicitacaoService.DecisaoForm(DecisaoOrcamento.PENDENTE)));
    // Simula a passagem de um dia no banco sem depender do relógio do teste.
    new TransactionTemplate(transactions).executeWithoutResult(tx -> entityManager.createQuery("update Orcamento o set o.dataValidade = :data where o.id = :id")
        .setParameter("data", LocalDate.now().minusDays(1)).setParameter("id", oid).executeUpdate());
    assertThrows(IllegalStateException.class, () -> servico.decidirOrcamento(id, oid, new SolicitacaoService.DecisaoForm(DecisaoOrcamento.APROVADO)));
    servico.iniciarRevisao(id, oid);
    servico.enviarOrcamento(id, form);
    Long atual = orcamentoAtual(id);
    mvc.perform(post(url + "/orcamentos/" + atual + "/decidir").with(csrf()).param("decisao", "RECUSADO"))
        .andExpect(flash().attributeExists("sucesso"));
    assertEquals(SituacaoSolicitacao.RECUSADA, solicitacoes.findById(id).orElseThrow().getSituacao());
    assertThrows(IllegalStateException.class, () -> servico.cancelar(id, new SolicitacaoService.CancelamentoForm("Teste")));
  }

  @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;

  private Long solicitacaoAprovada() {
    Long id = servico.abrir(new SolicitacaoService.AberturaForm(aeronave().getId(), TipoServico.REPARO, "Reparo completo")).getId();
    servico.iniciarAnalise(id);
    servico.enviarOrcamento(id, new SolicitacaoService.OrcamentoForm("Reparo", new BigDecimal("100.00"), LocalDate.now()));
    servico.decidirOrcamento(id, orcamentoAtual(id), new SolicitacaoService.DecisaoForm(DecisaoOrcamento.APROVADO));
    return id;
  }

  @Test
  void encaminhamentoEConfirmacaoConcluemFluxoComHistorico() throws Exception {
    Long id = solicitacaoAprovada();
    String url = "/solicitacoes/" + id;
    mvc.perform(post(url + "/encaminhar").with(csrf()).param("descricao", "Enviado à oficina para execução."))
        .andExpect(flash().attributeExists("sucesso"));
    assertEquals(SituacaoSolicitacao.ENCAMINHADA, solicitacoes.findById(id).orElseThrow().getSituacao());
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("Confirmar realização")));
    assertThrows(IllegalStateException.class, () -> servico.cancelar(id, new SolicitacaoService.CancelamentoForm("Tentativa")));
    mvc.perform(post(url + "/confirmar").with(csrf()).param("dataRealizacao", LocalDate.now().toString()).param("referencia", "OS-123"))
        .andExpect(flash().attributeExists("sucesso"));
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("OS-123")));
    new TransactionTemplate(transactions).executeWithoutResult(tx -> {
      var s = solicitacoes.findById(id).orElseThrow();
      assertEquals(SituacaoSolicitacao.CONCLUIDA, s.getSituacao());
      assertEquals("Enviado à oficina para execução.", s.getDescricaoEncaminhamento());
      assertNotNull(s.getDataHoraEncaminhamento());
      assertEquals(LocalDate.now(), s.getConfirmacao().getDataRealizacaoInformada());
      assertNotNull(s.getConfirmacao().getDataHoraRegistro());
      assertEquals(6, s.getHistorico().size());
      assertEquals(s.getConfirmacao().getResponsavel().getId(), s.getHistorico().get(5).getAutor().getId());
    });
    assertThrows(IllegalStateException.class, () -> servico.confirmar(id, new SolicitacaoService.ConfirmacaoForm(LocalDate.now(), null)));
    assertThrows(IllegalStateException.class, () -> servico.encaminhar(id, new SolicitacaoService.EncaminhamentoForm("Reenvio")));
    assertThrows(IllegalStateException.class, () -> servico.cancelar(id, new SolicitacaoService.CancelamentoForm("Tentativa")));
  }

  @Test
  void finalizacaoRejeitaEtapasIncorretasEDatasInvalidas() throws Exception {
    Long aberta = servico.abrir(new SolicitacaoService.AberturaForm(aeronave().getId(), TipoServico.OUTRO, "Teste")).getId();
    assertThrows(IllegalStateException.class, () -> servico.encaminhar(aberta, new SolicitacaoService.EncaminhamentoForm("Oficina")));
    Long id = solicitacaoAprovada();
    String url = "/solicitacoes/" + id;
    assertThrows(IllegalStateException.class, () -> servico.confirmar(id, new SolicitacaoService.ConfirmacaoForm(LocalDate.now(), null)));
    for (String descricao : new String[]{" ", "x".repeat(2001)})
      mvc.perform(post(url + "/encaminhar").with(csrf()).param("descricao", descricao))
          .andExpect(flash().attributeExists("erro"));
    mvc.perform(post(url + "/encaminhar").param("descricao", "Oficina")).andExpect(status().isForbidden());
    servico.encaminhar(id, new SolicitacaoService.EncaminhamentoForm("Oficina"));
    for (String data : new String[]{"", "invalida", LocalDate.now().plusDays(1).toString(), LocalDate.now().minusDays(1).toString()})
      mvc.perform(post(url + "/confirmar").with(csrf()).param("dataRealizacao", data))
          .andExpect(flash().attributeExists("erro"));
    mvc.perform(post(url + "/confirmar").with(csrf()).param("dataRealizacao", LocalDate.now().toString()).param("referencia", "x".repeat(501)))
        .andExpect(flash().attributeExists("erro"));
    assertEquals(SituacaoSolicitacao.ENCAMINHADA, solicitacoes.findById(id).orElseThrow().getSituacao());
    servico.confirmar(id, new SolicitacaoService.ConfirmacaoForm(LocalDate.now(), null));
    assertEquals(SituacaoSolicitacao.CONCLUIDA, solicitacoes.findById(id).orElseThrow().getSituacao());
  }

  @Test
  void complementosPersistemPerguntaRespostaTransicoesEAutores() throws Exception {
    var a = aeronave();
    Long id = servico.abrir(new SolicitacaoService.AberturaForm(a.getId(), TipoServico.INSPECAO, "Inspeção")).getId();
    String url = "/solicitacoes/" + id;
    servico.iniciarAnalise(id);
    mvc.perform(post(url + "/complementos").with(csrf()).param("pergunta", "Quando ocorreu a última inspeção?"))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    Long cid = complementoPendente(id);
    assertEquals(SituacaoSolicitacao.AGUARDANDO_INFORMACAO, solicitacoes.findById(id).orElseThrow().getSituacao());
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("Quando ocorreu a última inspeção?")));
    mvc.perform(post(url + "/complementos/" + cid + "/responder").with(csrf()).param("resposta", "Em outubro."))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("Em outubro.")));
    new TransactionTemplate(transactions).executeWithoutResult(tx -> {
      var s = solicitacoes.findById(id).orElseThrow();
      assertEquals(SituacaoSolicitacao.EM_ANALISE, s.getSituacao());
      assertEquals(4, s.getHistorico().size());
      assertEquals("Em outubro.", s.getComplementos().get(0).getResposta());
      assertNotNull(s.getComplementos().get(0).getDataHoraResposta());
      assertEquals(s.getCliente().getId(), s.getHistorico().get(3).getAutor().getId());
      assertEquals(gestores.findFirstByOrderByIdAsc().orElseThrow().getId(), s.getHistorico().get(2).getAutor().getId());
    });
    servico.solicitarComplemento(id, new SolicitacaoService.PerguntaForm("Qual o total de horas?"));
    assertThrows(IllegalStateException.class, () -> servico.responderComplemento(id, cid, new SolicitacaoService.RespostaForm("Alteração indevida")));
    assertEquals(SituacaoSolicitacao.AGUARDANDO_INFORMACAO, solicitacoes.findById(id).orElseThrow().getSituacao());
    servico.responderComplemento(id, complementoPendente(id), new SolicitacaoService.RespostaForm("120 horas"));
  }

  @Test
  void complementosRejeitamEstadoInvalidoEntradaVaziaDuplicidadeEVinculoIncorreto() throws Exception {
    var a = aeronave();
    Long id = servico.abrir(new SolicitacaoService.AberturaForm(a.getId(), TipoServico.REPARO, "Reparo")).getId();
    var pergunta = new SolicitacaoService.PerguntaForm("Qual o defeito?");
    assertThrows(IllegalStateException.class, () -> servico.solicitarComplemento(id, pergunta));
    servico.iniciarAnalise(id);
    String url = "/solicitacoes/" + id;
    mvc.perform(post(url + "/complementos").with(csrf()).param("pergunta", " "))
        .andExpect(flash().attributeExists("erro"));
    mvc.perform(post(url + "/complementos").with(csrf()).param("pergunta", "x".repeat(2001)))
        .andExpect(flash().attributeExists("erro"));
    mvc.perform(post(url + "/complementos").param("pergunta", "Teste")).andExpect(status().isForbidden());
    assertEquals(SituacaoSolicitacao.EM_ANALISE, solicitacoes.findById(id).orElseThrow().getSituacao());
    servico.solicitarComplemento(id, pergunta);
    Long cid = complementoPendente(id);
    assertThrows(IllegalStateException.class, () -> servico.solicitarComplemento(id, pergunta));
    mvc.perform(post(url + "/complementos/" + cid + "/responder").with(csrf()).param("resposta", " "))
        .andExpect(flash().attributeExists("erro"));
    mvc.perform(post(url + "/complementos/" + cid + "/responder").with(csrf()).param("resposta", "x".repeat(2001)))
        .andExpect(flash().attributeExists("erro"));
    Long outroId = servico.abrir(new SolicitacaoService.AberturaForm(a.getId(), TipoServico.OUTRO, "Outra solicitação")).getId();
    servico.iniciarAnalise(outroId);
    servico.solicitarComplemento(outroId, pergunta);
    assertThrows(IllegalArgumentException.class, () -> servico.responderComplemento(outroId, cid, new SolicitacaoService.RespostaForm("Resposta")));
    servico.cancelar(id, new SolicitacaoService.CancelamentoForm("Não necessário"));
    assertThrows(IllegalStateException.class, () -> servico.responderComplemento(id, cid, new SolicitacaoService.RespostaForm("Tardia")));
    assertNull(complementos.findById(cid).orElseThrow().getResposta());
  }

  @Test
  void fluxoDaAberturaAoCancelamentoPersisteHistoricoERecusaReabertura() throws Exception {
    Aeronave a = aeronave();
    var resposta =
        mvc.perform(
                post("/solicitacoes")
                    .with(csrf())
                    .param("aeronaveId", a.getId().toString())
                    .param("tipoServico", "INSPECAO")
                    .param("descricao", "Inspeção periódica do conjunto"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    String url = resposta.getResponse().getRedirectedUrl();
    Long id = Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Inspeção periódica")));
    mvc.perform(post(url + "/analisar").with(csrf())).andExpect(status().is3xxRedirection());
    assertEquals(
        SituacaoSolicitacao.EM_ANALISE, solicitacoes.findById(id).orElseThrow().getSituacao());
    mvc.perform(post(url + "/cancelar").with(csrf()).param("motivo", " "))
        .andExpect(flash().attributeExists("erro"));
    assertEquals(
        SituacaoSolicitacao.EM_ANALISE, solicitacoes.findById(id).orElseThrow().getSituacao());
    mvc.perform(post(url + "/cancelar").with(csrf()).param("motivo", "Serviço não será necessário"))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Serviço não será necessário")));
    assertThrows(IllegalStateException.class, () -> servico.iniciarAnalise(id));
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx -> {
              Solicitacao s = solicitacoes.findById(id).orElseThrow();
              assertEquals(SituacaoSolicitacao.CANCELADA, s.getSituacao());
              assertEquals(3, s.getHistorico().size());
              assertEquals(s.getCliente().getId(), s.getHistorico().get(0).getAutor().getId());
              assertEquals(
                  SituacaoSolicitacao.EM_ANALISE, s.getHistorico().get(2).getSituacaoAnterior());
            });
  }

  @Test
  void validacaoDuplicidadeFiltrosEProtecaoDosFormularios() throws Exception {
    Aeronave a = aeronave();
    mvc.perform(post("/clientes").param("nome", "Sem token")).andExpect(status().isForbidden());
    mvc.perform(
            post("/clientes")
                .with(csrf())
                .param("nome", "")
                .param("email", "invalido")
                .param("documento", ""))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("form"));
    mvc.perform(
            post("/aeronaves")
                .with(csrf())
                .param("matricula", a.getMatricula().toLowerCase())
                .param("fabricante", "Fabricante")
                .param("modelo", "Modelo")
                .param("clienteId", a.getCliente().getId().toString()))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("form"));
    var s =
        servico.abrir(
            new SolicitacaoService.AberturaForm(
                a.getId(), TipoServico.REPARO, "Reparo para pesquisa"));
    mvc.perform(get("/solicitacoes").param("busca", a.getMatricula()).param("situacao", "ABERTA"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("SG-" + s.getId())));
    mvc.perform(
            get("/solicitacoes").param("busca", a.getMatricula()).param("situacao", "CANCELADA"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Nenhuma solicitação encontrada")));
    mvc.perform(get("/solicitacoes/99999999")).andExpect(status().isNotFound());
    for (String url : new String[] {"/", "/clientes", "/aeronaves", "/solicitacoes/nova"})
      mvc.perform(get(url)).andExpect(status().isOk());
  }

  @Test
  void entidadesDasProximasEtapasPodemSerPersistidasComSeusRelacionamentos() {
    Aeronave a = aeronave();
    Long id =
        servico
            .abrir(
                new SolicitacaoService.AberturaForm(
                    a.getId(), TipoServico.OUTRO, "Mapeamento do domínio"))
            .getId();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx -> {
              Solicitacao s = solicitacoes.findById(id).orElseThrow();
              var gestor = gestores.findFirstByOrderByIdAsc().orElseThrow();
              var o =
                  orcamentos.save(
                      new Orcamento(
                          s, 1, "Serviço", new BigDecimal("1250.50"), LocalDate.now(), gestor));
              assertTrue(o.podeSerDecidido(LocalDate.now()));
              assertTrue(o.estaVencido(LocalDate.now().plusDays(1)));
              var c = complementos.save(new Complemento(s, "Qual a última inspeção?"));
              c.responder("Há seis meses");
              var responsavel =
                  responsaveis.save(
                      new ResponsavelConfirmacao(
                          "Responsável teste", UUID.randomUUID() + "@exemplo.com"));
              assertNotNull(
                  confirmacoes
                      .save(new Confirmacao(s, LocalDate.now(), "Referência de teste", responsavel))
                      .getId());
              assertNotNull(o.getId());
              assertNotNull(c.getId());
            });
  }
}
