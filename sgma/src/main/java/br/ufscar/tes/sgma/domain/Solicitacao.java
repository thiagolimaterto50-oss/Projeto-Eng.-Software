package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class Solicitacao extends Entidade {
  @Version private long versaoRegistro;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Cliente cliente;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Aeronave aeronave;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoServico tipoServico;

  @Column(nullable = false, length = 2000)
  private String descricao;

  private String anexo;

  @Column(nullable = false)
  private LocalDateTime dataHoraAbertura;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SituacaoSolicitacao situacao;

  @Column(length = 2000)
  private String observacoesTriagem;

  @Column(length = 2000)
  private String descricaoEncaminhamento;

  private LocalDateTime dataHoraEncaminhamento;

  @Column(length = 1000)
  private String motivoCancelamento;

  @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL)
  @OrderBy("dataHora ASC, id ASC")
  private List<EventoHistorico> historico = new ArrayList<>();

  @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL)
  @OrderBy("dataHoraPergunta ASC, id ASC")
  private List<Complemento> complementos = new ArrayList<>();

  @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL)
  @OrderBy("versao ASC")
  private List<Orcamento> orcamentos = new ArrayList<>();

  @OneToOne(mappedBy = "solicitacao", cascade = CascadeType.ALL)
  private Confirmacao confirmacao;

  protected Solicitacao() {}

  public Solicitacao(Aeronave aeronave, TipoServico tipoServico, String descricao) {
    if (aeronave == null || !aeronave.isAtiva() || !aeronave.getCliente().isAtivo())
      throw new IllegalArgumentException("Selecione uma aeronave ativa de um cliente ativo.");
    if (tipoServico == null) throw new IllegalArgumentException("Selecione o tipo de serviço.");
    if (descricao == null || descricao.isBlank() || descricao.length() > 2000)
      throw new IllegalArgumentException("Informe uma descrição de até 2000 caracteres.");
    this.aeronave = aeronave;
    this.cliente = aeronave.getCliente();
    this.tipoServico = tipoServico;
    this.descricao = descricao.strip();
    this.dataHoraAbertura = LocalDateTime.now();
    this.situacao = SituacaoSolicitacao.ABERTA;
    historico.add(new EventoHistorico(this, null, situacao, "Solicitação aberta.", cliente));
  }

  public void iniciarAnalise(GestorSolicitacoes gestor) {
    if (situacao != SituacaoSolicitacao.ABERTA)
      throw new IllegalStateException("Somente solicitações abertas podem iniciar a análise.");
    transicionar(SituacaoSolicitacao.EM_ANALISE, "Análise iniciada.", gestor);
  }

  public void solicitarComplemento(String pergunta, GestorSolicitacoes gestor) {
    if (situacao != SituacaoSolicitacao.EM_ANALISE)
      throw new IllegalStateException("Só é possível pedir complemento durante a análise.");
    if (complementos.stream().anyMatch(c -> c.getResposta() == null))
      throw new IllegalStateException("Já existe uma pergunta aguardando resposta.");
    Complemento complemento = new Complemento(this, pergunta);
    transicionar(SituacaoSolicitacao.AGUARDANDO_INFORMACAO, "Complemento solicitado.", gestor);
    complementos.add(complemento);
  }

  public void responderComplemento(Long complementoId, String resposta, Cliente autor) {
    if (situacao != SituacaoSolicitacao.AGUARDANDO_INFORMACAO)
      throw new IllegalStateException("Esta solicitação não está aguardando informações.");
    if (autor == null
        || !autor.isAtivo()
        || (autor != cliente
            && (cliente.getId() == null || !cliente.getId().equals(autor.getId()))))
      throw new IllegalArgumentException("Somente o cliente da solicitação pode responder.");
    Complemento complemento =
        complementos.stream()
            .filter(c -> complementoId != null && complementoId.equals(c.getId()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Complemento não pertence a esta solicitação."));
    complemento.responder(resposta);
    transicionar(
        SituacaoSolicitacao.EM_ANALISE, "Complemento respondido. Análise retomada.", autor);
  }

  public boolean isCancelavel() {
    return switch (situacao) {
      case ABERTA, EM_ANALISE, AGUARDANDO_INFORMACAO, ORCAMENTO_ENVIADO, APROVADA -> true;
      default -> false;
    };
  }

  public void encaminhar(String descricao, GestorSolicitacoes gestor) {
    if (situacao != SituacaoSolicitacao.APROVADA)
      throw new IllegalStateException("Somente solicitações aprovadas podem ser encaminhadas.");
    if (descricao == null || descricao.isBlank() || descricao.length() > 2000)
      throw new IllegalArgumentException("Informe o encaminhamento, até 2000 caracteres.");
    transicionar(SituacaoSolicitacao.ENCAMINHADA, "Serviço encaminhado.", gestor);
    descricaoEncaminhamento = descricao.strip();
    dataHoraEncaminhamento = LocalDateTime.now();
  }

  public void confirmar(
      java.time.LocalDate data, String referencia, ResponsavelConfirmacao responsavel) {
    if (situacao != SituacaoSolicitacao.ENCAMINHADA || confirmacao != null)
      throw new IllegalStateException(
          "Somente solicitações encaminhadas podem ser confirmadas uma única vez.");
    if (data == null || data.isBefore(dataHoraEncaminhamento.toLocalDate()))
      throw new IllegalArgumentException("A realização não pode ser anterior ao encaminhamento.");
    Confirmacao nova = new Confirmacao(this, data, referencia, responsavel);
    transicionar(SituacaoSolicitacao.CONCLUIDA, "Realização do serviço confirmada.", responsavel);
    confirmacao = nova;
  }

  public String getDescricaoEncaminhamento() {
    return descricaoEncaminhamento;
  }

  public LocalDateTime getDataHoraEncaminhamento() {
    return dataHoraEncaminhamento;
  }

  public void enviarOrcamento(
      String descricao,
      java.math.BigDecimal valor,
      java.time.LocalDate validade,
      GestorSolicitacoes gestor) {
    if (situacao != SituacaoSolicitacao.EM_ANALISE)
      throw new IllegalStateException("O orçamento só pode ser enviado durante a análise.");
    int versao = orcamentos.stream().mapToInt(Orcamento::getVersao).max().orElse(0) + 1;
    Orcamento novo = new Orcamento(this, versao, descricao, valor, validade, gestor);
    transicionar(
        SituacaoSolicitacao.ORCAMENTO_ENVIADO, "Orçamento v" + versao + " enviado.", gestor);
    orcamentos.forEach(Orcamento::substituir);
    orcamentos.add(novo);
  }

  public void iniciarRevisao(Long orcamentoId, GestorSolicitacoes gestor) {
    Orcamento atual = orcamentoParaDecisaoOuRevisao(orcamentoId);
    transicionar(
        SituacaoSolicitacao.EM_ANALISE,
        "Revisão do orçamento v" + atual.getVersao() + " iniciada.",
        gestor);
    atual.substituir();
  }

  public void decidirOrcamento(Long orcamentoId, DecisaoOrcamento decisao, Cliente autor) {
    if (autor == null
        || !autor.isAtivo()
        || (autor != cliente
            && (cliente.getId() == null || !cliente.getId().equals(autor.getId()))))
      throw new IllegalArgumentException("Somente o cliente da solicitação pode decidir.");
    Orcamento atual = orcamentoParaDecisaoOuRevisao(orcamentoId);
    atual.decidir(decisao, autor);
    boolean aprovado = decisao == DecisaoOrcamento.APROVADO;
    transicionar(
        aprovado ? SituacaoSolicitacao.APROVADA : SituacaoSolicitacao.RECUSADA,
        "Orçamento v" + atual.getVersao() + (aprovado ? " aprovado." : " recusado."),
        autor);
  }

  private Orcamento orcamentoParaDecisaoOuRevisao(Long id) {
    if (situacao != SituacaoSolicitacao.ORCAMENTO_ENVIADO)
      throw new IllegalStateException("Esta solicitação não está aguardando decisão de orçamento.");
    Orcamento atual = getOrcamentoVigente();
    if (atual == null || id == null || !id.equals(atual.getId()))
      throw new IllegalArgumentException(
          "Este orçamento não é a versão vigente desta solicitação.");
    return atual;
  }

  public void cancelar(String motivo, Usuario autor) {
    if (!isCancelavel())
      throw new IllegalStateException("Esta solicitação não pode ser cancelada.");
    if (motivo == null || motivo.isBlank() || motivo.length() > 1000)
      throw new IllegalArgumentException("Informe um motivo de até 1000 caracteres.");
    if (!(autor instanceof GestorSolicitacoes)
        && autor != cliente
        && !(autor instanceof Cliente
            && cliente.getId() != null
            && cliente.getId().equals(autor.getId())))
      throw new IllegalArgumentException(
          "Somente o cliente da solicitação ou o gestor pode cancelar.");
    transicionar(SituacaoSolicitacao.CANCELADA, "Cancelamento: " + motivo.strip(), autor);
    motivoCancelamento = motivo.strip();
  }

  private void transicionar(SituacaoSolicitacao destino, String descricao, Usuario autor) {
    if (autor == null || !autor.isAtivo()) throw new IllegalArgumentException("Autor inválido.");
    historico.add(new EventoHistorico(this, situacao, destino, descricao, autor));
    situacao = destino;
  }

  public String getObservacoesTriagem() {
    return observacoesTriagem;
  }

  public void registrarTriagem(String observacoes, GestorSolicitacoes gestor) {
    if (situacao != SituacaoSolicitacao.EM_ANALISE)
      throw new IllegalStateException("Registre a triagem durante a análise.");
    if (observacoes == null || observacoes.isBlank() || observacoes.length() > 2000)
      throw new IllegalArgumentException("Informe observações de até 2000 caracteres.");
    transicionar(situacao, "Observações de triagem atualizadas.", gestor);
    observacoesTriagem = observacoes.strip();
  }

  public void registrarAnexo(String nome, Usuario autor) {
    if (!isCancelavel())
      throw new IllegalStateException(
          "Não é possível alterar o anexo após encaminhamento ou encerramento.");
    transicionar(situacao, "Anexo atualizado: " + nome, autor);
    anexo = nome;
  }

  public String getAnexo() {
    return anexo;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public Aeronave getAeronave() {
    return aeronave;
  }

  public TipoServico getTipoServico() {
    return tipoServico;
  }

  public String getDescricao() {
    return descricao;
  }

  public LocalDateTime getDataHoraAbertura() {
    return dataHoraAbertura;
  }

  public SituacaoSolicitacao getSituacao() {
    return situacao;
  }

  public String getMotivoCancelamento() {
    return motivoCancelamento;
  }

  public List<EventoHistorico> getHistorico() {
    return Collections.unmodifiableList(historico);
  }

  public List<Complemento> getComplementos() {
    return Collections.unmodifiableList(complementos);
  }

  public List<Orcamento> getOrcamentos() {
    return Collections.unmodifiableList(orcamentos);
  }

  public Confirmacao getConfirmacao() {
    return confirmacao;
  }

  public Orcamento getOrcamentoVigente() {
    return orcamentos.stream().filter(Orcamento::isVigente).findFirst().orElse(null);
  }
}
