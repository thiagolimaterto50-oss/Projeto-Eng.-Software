package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"solicitacao_id", "versao"}))
public class Orcamento extends Entidade {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "solicitacao_id", nullable = false)
  private Solicitacao solicitacao;

  @Column(nullable = false)
  private int versao;

  @Column(nullable = false, length = 2000)
  private String descricaoServico;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal valorTotal;

  @Column(nullable = false)
  private LocalDate dataValidade;

  @Column(nullable = false)
  private LocalDateTime dataHoraEnvio;

  @Column(nullable = false)
  private boolean vigente = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DecisaoOrcamento decisao = DecisaoOrcamento.PENDENTE;

  private LocalDateTime dataHoraDecisao;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private GestorSolicitacoes elaborador;

  @ManyToOne(fetch = FetchType.LAZY)
  private Cliente decisor;

  protected Orcamento() {}

  public Orcamento(
      Solicitacao solicitacao,
      int versao,
      String descricao,
      BigDecimal valor,
      LocalDate validade,
      GestorSolicitacoes elaborador) {
    if (descricao == null || descricao.isBlank() || descricao.length() > 2000)
      throw new IllegalArgumentException("Informe a descrição do orçamento, até 2000 caracteres.");
    if (valor == null || valor.signum() <= 0 || valor.scale() > 2 || valor.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new IllegalArgumentException("Informe um valor positivo com até duas casas decimais.");
    if (validade == null || validade.isBefore(LocalDate.now()))
      throw new IllegalArgumentException("A validade não pode ser anterior a hoje.");
    if (solicitacao == null || versao < 1 || elaborador == null || !elaborador.isAtivo())
      throw new IllegalArgumentException("Dados de elaboração inválidos.");
    this.solicitacao = solicitacao;
    this.versao = versao;
    this.descricaoServico = descricao.strip();
    this.valorTotal = valor;
    this.dataValidade = validade;
    this.elaborador = elaborador;
    this.dataHoraEnvio = LocalDateTime.now();
  }

  public boolean estaVencido(LocalDate hoje) {
    return dataValidade.isBefore(hoje);
  }

  public boolean podeSerDecidido(LocalDate hoje) {
    return vigente && decisao == DecisaoOrcamento.PENDENTE && !estaVencido(hoje);
  }

  void substituir() {
    vigente = false;
  }

  void decidir(DecisaoOrcamento decisao, Cliente cliente) {
    if (decisao != DecisaoOrcamento.APROVADO && decisao != DecisaoOrcamento.RECUSADO)
      throw new IllegalArgumentException("Selecione aprovar ou recusar.");
    if (!podeSerDecidido(LocalDate.now()))
      throw new IllegalStateException("Orçamento indisponível para decisão.");
    this.decisao = decisao;
    this.decisor = cliente;
    this.dataHoraDecisao = LocalDateTime.now();
  }

  public Solicitacao getSolicitacao() {
    return solicitacao;
  }

  public boolean isVencido() { return estaVencido(LocalDate.now()); }
  public boolean isDisponivelParaDecisao() { return podeSerDecidido(LocalDate.now()); }

  public int getVersao() {
    return versao;
  }

  public String getDescricaoServico() {
    return descricaoServico;
  }

  public BigDecimal getValorTotal() {
    return valorTotal;
  }

  public LocalDate getDataValidade() {
    return dataValidade;
  }

  public LocalDateTime getDataHoraEnvio() {
    return dataHoraEnvio;
  }

  public boolean isVigente() {
    return vigente;
  }

  public DecisaoOrcamento getDecisao() {
    return decisao;
  }

  public LocalDateTime getDataHoraDecisao() {
    return dataHoraDecisao;
  }

  public GestorSolicitacoes getElaborador() {
    return elaborador;
  }

  public Cliente getDecisor() {
    return decisor;
  }
}
