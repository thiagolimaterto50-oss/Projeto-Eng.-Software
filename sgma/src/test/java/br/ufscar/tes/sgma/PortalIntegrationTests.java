package br.ufscar.tes.sgma;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import br.ufscar.tes.sgma.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired IUsuarioDAO usuarios;
  @Autowired IClienteDAO clientes;
  @Autowired IAeronaveDAO aeronaves;
  @Autowired ISolicitacaoDAO solicitacoes;
  @Autowired IComplementoDAO complementos;
  @Autowired IOrcamentoDAO orcamentos;
  @Autowired IAnexoDAO anexos;
  @Autowired PasswordEncoder encoder;
  @Autowired SolicitacaoService service;
  @Autowired PlatformTransactionManager transactions;
  @PersistenceContext EntityManager em;
  Cliente cliente, outro;
  GestorSolicitacoes gestor;
  ResponsavelConfirmacao responsavel;
  Aeronave aeronave, aeronaveOutro;
  final String senha = "SenhaTeste123!";

  @BeforeEach
  void dados() {
    String chave = UUID.randomUUID().toString().substring(0, 8);
    String hash = encoder.encode(senha);
    cliente = new Cliente("Cliente " + chave, chave + "@cliente.test", chave, "123");
    outro = new Cliente("Outro " + chave, chave + "@outro.test", chave + "B", "123");
    gestor = new GestorSolicitacoes("Gestor " + chave, chave + "@gestor.test");
    responsavel = new ResponsavelConfirmacao("Responsável " + chave, chave + "@responsavel.test");
    for (Usuario u : List.of(cliente, outro, gestor, responsavel)) {
      u.definirSenhaHash(hash);
      usuarios.save(u);
    }
    aeronave = aeronaves.save(new Aeronave("PT-" + chave, "Embraer", "Modelo", cliente));
    aeronaveOutro = aeronaves.save(new Aeronave("PR-" + chave, "Embraer", "Modelo", outro));
  }

  @AfterEach
  void limparSessao() {
    SecurityContextHolder.clearContext();
  }

  ResultActions como(Usuario u, AbstractMockHttpServletRequestBuilder<?> req) throws Exception {
    return mvc.perform(req.with(user(u.getEmail()).roles(u.getPerfil())).with(csrf()));
  }

  void ok(Usuario u, AbstractMockHttpServletRequestBuilder<?> req) throws Exception {
    como(u, req)
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attributeExists("sucesso"));
  }

  Long abrir(Cliente c, Aeronave a) throws Exception {
    String url =
        como(
                c,
                post("/solicitacoes")
                    .param("aeronaveId", a.getId().toString())
                    .param("tipoServico", "REPARO")
                    .param("descricao", "Reparo de teste"))
            .andExpect(flash().attributeExists("sucesso"))
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
  }

  String url(Long id) {
    return "/solicitacoes/" + id;
  }

  Long vigente(Long id) {
    return new TransactionTemplate(transactions)
        .execute(tx -> solicitacoes.findById(id).orElseThrow().getOrcamentoVigente().getId());
  }

  Long pendente(Long id) {
    return new TransactionTemplate(transactions)
        .execute(
            tx ->
                solicitacoes.findById(id).orElseThrow().getComplementos().stream()
                    .filter(c -> c.getResposta() == null)
                    .findFirst()
                    .orElseThrow()
                    .getId());
  }

  void enviar(Long id, String valor) throws Exception {
    ok(
        gestor,
        post(url(id) + "/orcamentos")
            .param("descricaoServico", "Reparo")
            .param("valorTotal", valor)
            .param("dataValidade", LocalDate.now().toString()));
  }

  Long aprovada() throws Exception {
    Long id = abrir(cliente, aeronave);
    ok(gestor, post(url(id) + "/analisar"));
    enviar(id, "150.50");
    ok(
        cliente,
        post(url(id) + "/orcamentos/" + vigente(id) + "/decidir").param("decisao", "APROVADO"));
    return id;
  }

  @Test
  void loginLogoutSenhaEContaInativa() throws Exception {
    mvc.perform(get("/painel")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", cliente.getEmail())
                .param("password", "errada"))
        .andExpect(redirectedUrl("/login?error"));
    var session =
        (org.springframework.mock.web.MockHttpSession)
            mvc.perform(
                    post("/login")
                        .with(csrf())
                        .param("username", cliente.getEmail())
                        .param("password", senha))
                .andExpect(redirectedUrl("/painel"))
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(get("/painel").session(session)).andExpect(status().isOk());
    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"));
    assertTrue(session.isInvalid());
    cliente.atualizar(cliente.getNome(), cliente.getEmail(), cliente.getDocumento(), "", false);
    clientes.save(cliente);
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", cliente.getEmail())
                .param("password", senha))
        .andExpect(redirectedUrl("/login?error"));
    como(cliente, get("/painel")).andExpect(status().isForbidden());
  }

  @Test
  void clienteEnxergaSomenteSeusRegistrosInclusiveContagens() throws Exception {
    Long meu = abrir(cliente, aeronave), alheio = abrir(outro, aeronaveOutro);
    como(cliente, get("/solicitacoes"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(aeronave.getMatricula())))
        .andExpect(content().string(not(containsString(aeronaveOutro.getMatricula()))));
    como(cliente, get("/painel"))
        .andExpect(model().attribute("total", 1L))
        .andExpect(model().attribute("aeronavesTotal", 1));
    como(cliente, get("/aeronaves"))
        .andExpect(content().string(not(containsString(aeronaveOutro.getMatricula()))));
    como(cliente, get(url(alheio))).andExpect(status().isForbidden());
    como(cliente, post(url(alheio) + "/cancelar").param("motivo", "Teste"))
        .andExpect(status().isForbidden());
    como(
            cliente,
            post("/solicitacoes")
                .param("aeronaveId", aeronaveOutro.getId().toString())
                .param("tipoServico", "OUTRO")
                .param("descricao", "Tentativa"))
        .andExpect(status().isForbidden());
    como(cliente, get("/clientes")).andExpect(status().isForbidden());
    como(cliente, get("/aeronaves/" + aeronaveOutro.getId() + "/editar"))
        .andExpect(status().isForbidden());
    como(gestor, get(url(meu))).andExpect(status().isOk());
  }

  @Test
  void fluxoCompletoComAutoresReaisComplementosOrcamentosEConfirmacao() throws Exception {
    Long id = abrir(cliente, aeronave);
    ok(gestor, post(url(id) + "/analisar"));
    ok(gestor, post(url(id) + "/triagem").param("observacoes", "Verificar documentação interna"));
    como(cliente, get(url(id)))
        .andExpect(content().string(not(containsString("Verificar documentação interna"))));
    ok(gestor, post(url(id) + "/complementos").param("pergunta", "Última inspeção?"));
    Long cid = pendente(id);
    ok(
        cliente,
        post(url(id) + "/complementos/" + cid + "/responder").param("resposta", "Em outubro"));
    enviar(id, "1500.50");
    Long anterior = vigente(id);
    ok(gestor, post(url(id) + "/orcamentos/" + anterior + "/revisar"));
    enviar(id, "1200.00");
    como(
            cliente,
            post(url(id) + "/orcamentos/" + anterior + "/decidir").param("decisao", "APROVADO"))
        .andExpect(flash().attributeExists("erro"));
    ok(
        cliente,
        post(url(id) + "/orcamentos/" + vigente(id) + "/decidir").param("decisao", "APROVADO"));
    ok(gestor, post(url(id) + "/encaminhar").param("descricao", "Oficina responsável"));
    ok(
        responsavel,
        post(url(id) + "/confirmar")
            .param("dataRealizacao", LocalDate.now().toString())
            .param("referencia", "OS-123"));
    como(cliente, get(url(id))).andExpect(content().string(containsString("OS-123")));
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx -> {
              var s = solicitacoes.findById(id).orElseThrow();
              assertEquals(SituacaoSolicitacao.CONCLUIDA, s.getSituacao());
              assertEquals(2, s.getOrcamentos().size());
              assertFalse(s.getOrcamentos().get(0).isVigente());
              assertEquals(cliente.getId(), s.getOrcamentoVigente().getDecisor().getId());
              assertEquals(responsavel.getId(), s.getConfirmacao().getResponsavel().getId());
              assertEquals(gestor.getId(), s.getOrcamentoVigente().getElaborador().getId());
              assertEquals(11, s.getHistorico().size());
              assertEquals(cliente.getId(), s.getHistorico().get(0).getAutor().getId());
            });
  }

  @Test
  void perfisNaoPodemExecutarAcoesUnsDosOutrosNemDiretoNoServico() throws Exception {
    Long id = abrir(cliente, aeronave);
    como(cliente, post(url(id) + "/analisar")).andExpect(status().isForbidden());
    como(responsavel, post(url(id) + "/analisar")).andExpect(status().isForbidden());
    como(responsavel, post(url(id) + "/cancelar").param("motivo", "Teste"))
        .andExpect(status().isForbidden());
    como(
            gestor,
            post("/solicitacoes")
                .param("aeronaveId", aeronave.getId().toString())
                .param("tipoServico", "OUTRO")
                .param("descricao", "Teste"))
        .andExpect(status().isForbidden());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(cliente.getEmail(), "", List.of()));
    assertThrows(AccessDeniedException.class, () -> service.iniciarAnalise(id));
    SecurityContextHolder.clearContext();
    ok(gestor, post(url(id) + "/analisar"));
    ok(gestor, post(url(id) + "/complementos").param("pergunta", "Detalhe?"));
    como(
            gestor,
            post(url(id) + "/complementos/" + pendente(id) + "/responder")
                .param("resposta", "Teste"))
        .andExpect(status().isForbidden());
    ok(
        cliente,
        post(url(id) + "/complementos/" + pendente(id) + "/responder")
            .param("resposta", "Resposta"));
    enviar(id, "50");
    como(
            gestor,
            post(url(id) + "/orcamentos/" + vigente(id) + "/decidir").param("decisao", "APROVADO"))
        .andExpect(status().isForbidden());
    ok(
        cliente,
        post(url(id) + "/orcamentos/" + vigente(id) + "/decidir").param("decisao", "APROVADO"));
    como(cliente, post(url(id) + "/encaminhar").param("descricao", "Oficina"))
        .andExpect(status().isForbidden());
    ok(gestor, post(url(id) + "/encaminhar").param("descricao", "Oficina"));
    como(gestor, post(url(id) + "/confirmar").param("dataRealizacao", LocalDate.now().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void cadastroEdicaoSenhaInativacaoEReativacao() throws Exception {
    String chave = UUID.randomUUID().toString().substring(0, 8);
    ok(
        gestor,
        post("/clientes")
            .param("nome", "Novo cliente")
            .param("email", chave + "@novo.test")
            .param("documento", chave)
            .param("senha", senha));
    var novo = usuarios.findByEmailIgnoreCase(chave + "@novo.test").orElseThrow();
    assertTrue(encoder.matches(senha, novo.getSenhaHash()));
    como(cliente, post("/clientes").param("nome", "Invasão")).andExpect(status().isForbidden());
    como(gestor, get("/clientes/" + cliente.getId() + "/editar")).andExpect(status().isOk());
    ok(
        gestor,
        post("/clientes/" + cliente.getId() + "/editar")
            .param("nome", "Nome alterado")
            .param("email", cliente.getEmail())
            .param("documento", cliente.getDocumento())
            .param("telefone", "987")
            .param("senha", "NovaSenha123!"));
    assertFalse(clientes.findById(cliente.getId()).orElseThrow().isAtivo());
    como(cliente, get("/painel")).andExpect(status().isForbidden());
    ok(
        gestor,
        post("/clientes/" + cliente.getId() + "/editar")
            .param("nome", "Nome alterado")
            .param("email", cliente.getEmail())
            .param("documento", cliente.getDocumento())
            .param("ativo", "true"));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", cliente.getEmail())
                .param("password", "NovaSenha123!"))
        .andExpect(redirectedUrl("/painel"));
  }

  @Test
  void aeronaveEditavelInativaBloqueiaAberturaEVinculoPreservaProprietario() throws Exception {
    como(gestor, get("/aeronaves/" + aeronave.getId() + "/editar")).andExpect(status().isOk());
    String edit = "/aeronaves/" + aeronave.getId() + "/editar";
    ok(
        gestor,
        post(edit)
            .param("matricula", aeronave.getMatricula())
            .param("fabricante", "Fabricante novo")
            .param("modelo", "Modelo novo")
            .param("clienteId", cliente.getId().toString())
            .param("justificativaVinculo", "Documento apresentado"));
    assertFalse(aeronaves.findById(aeronave.getId()).orElseThrow().isAtiva());
    como(
            cliente,
            post("/solicitacoes")
                .param("aeronaveId", aeronave.getId().toString())
                .param("tipoServico", "OUTRO")
                .param("descricao", "Teste"))
        .andExpect(model().attributeHasErrors("form"));
    como(
            gestor,
            post(edit)
                .param("matricula", aeronave.getMatricula())
                .param("fabricante", "Fabricante")
                .param("modelo", "Modelo")
                .param("clienteId", outro.getId().toString())
                .param("ativa", "true"))
        .andExpect(model().attributeHasErrors("form"));
    ok(
        gestor,
        post(edit)
            .param("matricula", aeronave.getMatricula())
            .param("fabricante", "Fabricante")
            .param("modelo", "Modelo")
            .param("clienteId", cliente.getId().toString())
            .param("ativa", "true")
            .param("justificativaVinculo", "Documento apresentado"));
    como(cliente, get("/aeronaves"))
        .andExpect(content().string(containsString("Documento apresentado")));
    abrir(cliente, aeronave);
  }

  @Test
  void anexoPersistidoDownloadProtegidoESubstituicao() throws Exception {
    Long id = abrir(cliente, aeronave);
    String path = url(id) + "/anexo";
    byte[] pdf = "%PDF-1.7 teste".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var file = new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", pdf);
    ok(cliente, multipart(path).file(file));
    como(cliente, get(path))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("attachment")))
        .andExpect(content().bytes(pdf));
    como(outro, get(path)).andExpect(status().isForbidden());
    como(outro, multipart(path).file(file)).andExpect(status().isForbidden());
    como(responsavel, multipart(path).file(file)).andExpect(status().isForbidden());
    ok(
        gestor,
        multipart(path)
            .file(
                new MockMultipartFile(
                    "arquivo", "novo.pdf", "application/pdf", "%PDF-1.7 novo".getBytes())));
    assertEquals("novo.pdf", anexos.findBySolicitacaoId(id).orElseThrow().getNome());
    ok(cliente, post(url(id) + "/cancelar").param("motivo", "Cancelado"));
    como(cliente, multipart(path).file(file)).andExpect(flash().attributeExists("erro"));
    como(cliente, get(path)).andExpect(status().isOk());
  }

  @Test
  void anexosRecusamFormatosETamanhosInvalidos() throws Exception {
    Long id = abrir(cliente, aeronave);
    for (MockMultipartFile file :
        List.of(
            new MockMultipartFile("arquivo", "x.pdf", "application/pdf", "invalido".getBytes()),
            new MockMultipartFile(
                "arquivo", "x.html", "text/html", "<script>alert(1)</script>".getBytes()),
            new MockMultipartFile(
                "arquivo", "x.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]),
            new MockMultipartFile("arquivo", "x.pdf", "application/pdf", new byte[0])))
      como(cliente, multipart(url(id) + "/anexo").file(file))
          .andExpect(flash().attributeExists("erro"));
    assertTrue(anexos.findBySolicitacaoId(id).isEmpty());
  }

  @Test
  void complementosInvalidosDuplicadosEOuOutraSolicitacaoNaoAlteramHistorico() throws Exception {
    Long id = abrir(cliente, aeronave);
    ok(gestor, post(url(id) + "/analisar"));
    for (String pergunta : List.of(" ", "x".repeat(2001)))
      como(gestor, post(url(id) + "/complementos").param("pergunta", pergunta))
          .andExpect(flash().attributeExists("erro"));
    ok(gestor, post(url(id) + "/complementos").param("pergunta", "Pergunta?"));
    Long cid = pendente(id);
    como(gestor, post(url(id) + "/complementos").param("pergunta", "Duplicada"))
        .andExpect(flash().attributeExists("erro"));
    como(cliente, post(url(id) + "/complementos/" + cid + "/responder").param("resposta", " "))
        .andExpect(flash().attributeExists("erro"));
    Long outroId = abrir(cliente, aeronave);
    ok(gestor, post(url(outroId) + "/analisar"));
    ok(gestor, post(url(outroId) + "/complementos").param("pergunta", "Outra?"));
    como(
            cliente,
            post(url(outroId) + "/complementos/" + cid + "/responder")
                .param("resposta", "Fora de contexto"))
        .andExpect(flash().attributeExists("erro"));
    ok(
        cliente,
        post(url(id) + "/complementos/" + cid + "/responder").param("resposta", "Respondido"));
    ok(gestor, post(url(id) + "/complementos").param("pergunta", "Nova rodada?"));
    como(
            cliente,
            post(url(id) + "/complementos/" + cid + "/responder").param("resposta", "Repetida"))
        .andExpect(flash().attributeExists("erro"));
    ok(cliente, post(url(id) + "/cancelar").param("motivo", "Não necessário"));
    como(
            cliente,
            post(url(id) + "/complementos/" + pendente(id) + "/responder")
                .param("resposta", "Tardia"))
        .andExpect(flash().attributeExists("erro"));
  }

  @Test
  void orcamentoVencidoValoresInvalidosERecusaFinal() throws Exception {
    Long id = abrir(cliente, aeronave);
    ok(gestor, post(url(id) + "/analisar"));
    for (String valor : List.of("-1", "0", "1.234", "10000000000", "abc"))
      como(
              gestor,
              post(url(id) + "/orcamentos")
                  .param("descricaoServico", "Reparo")
                  .param("valorTotal", valor)
                  .param("dataValidade", LocalDate.now().toString()))
          .andExpect(flash().attributeExists("erro"));
    enviar(id, "50");
    Long oid = vigente(id);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx ->
                em.createQuery("update Orcamento o set o.dataValidade=:data where o.id=:id")
                    .setParameter("data", LocalDate.now().minusDays(1))
                    .setParameter("id", oid)
                    .executeUpdate());
    como(cliente, post(url(id) + "/orcamentos/" + oid + "/decidir").param("decisao", "APROVADO"))
        .andExpect(flash().attributeExists("erro"));
    ok(gestor, post(url(id) + "/orcamentos/" + oid + "/revisar"));
    enviar(id, "50");
    ok(
        cliente,
        post(url(id) + "/orcamentos/" + vigente(id) + "/decidir").param("decisao", "RECUSADO"));
    assertEquals(
        SituacaoSolicitacao.RECUSADA, solicitacoes.findById(id).orElseThrow().getSituacao());
    como(gestor, post(url(id) + "/cancelar").param("motivo", "Teste"))
        .andExpect(flash().attributeExists("erro"));
  }

  @Test
  void finalizacaoValidaDatasEBloqueiaRepeticaoECancelamento() throws Exception {
    Long id = aprovada();
    como(
            responsavel,
            post(url(id) + "/confirmar").param("dataRealizacao", LocalDate.now().toString()))
        .andExpect(flash().attributeExists("erro"));
    ok(gestor, post(url(id) + "/encaminhar").param("descricao", "Oficina"));
    como(cliente, post(url(id) + "/cancelar").param("motivo", "Teste"))
        .andExpect(flash().attributeExists("erro"));
    for (String data :
        List.of(
            "",
            "invalida",
            LocalDate.now().minusDays(1).toString(),
            LocalDate.now().plusDays(1).toString()))
      como(responsavel, post(url(id) + "/confirmar").param("dataRealizacao", data))
          .andExpect(flash().attributeExists("erro"));
    ok(
        responsavel,
        post(url(id) + "/confirmar").param("dataRealizacao", LocalDate.now().toString()));
    como(
            responsavel,
            post(url(id) + "/confirmar").param("dataRealizacao", LocalDate.now().toString()))
        .andExpect(flash().attributeExists("erro"));
    como(gestor, post(url(id) + "/encaminhar").param("descricao", "Outra oficina"))
        .andExpect(flash().attributeExists("erro"));
  }

  @Test
  void senhaPessoalValidaSenhaAtualEMantemHash() throws Exception {
    como(
            cliente,
            post("/conta/senha").param("senhaAtual", "errada").param("novaSenha", "OutraSenha123!"))
        .andExpect(flash().attributeExists("erro"));
    ok(
        cliente,
        post("/conta/senha").param("senhaAtual", senha).param("novaSenha", "OutraSenha123!"));
    var hash = usuarios.findById(cliente.getId()).orElseThrow().getSenhaHash();
    assertTrue(encoder.matches("OutraSenha123!", hash));
    assertNotEquals("OutraSenha123!", hash);
  }

  @Test
  void csrfCadastroDuplicadoETelas() throws Exception {
    mvc.perform(post("/clientes").with(user(gestor.getEmail()).roles("GESTOR")))
        .andExpect(status().isForbidden());
    como(
            gestor,
            post("/aeronaves")
                .param("matricula", aeronave.getMatricula().toLowerCase())
                .param("fabricante", "Embraer")
                .param("modelo", "Modelo")
                .param("clienteId", cliente.getId().toString()))
        .andExpect(model().attributeHasErrors("form"));
    for (String rota : List.of("/painel", "/clientes", "/aeronaves", "/solicitacoes", "/conta"))
      como(gestor, get(rota)).andExpect(status().isOk());
    como(cliente, get("/solicitacoes/nova")).andExpect(status().isOk());
    como(cliente, get("/solicitacoes/99999999")).andExpect(status().isNotFound());
  }
}
