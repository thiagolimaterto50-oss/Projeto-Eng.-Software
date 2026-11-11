package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class EventoHistorico extends Entidade {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Solicitacao solicitacao;

  @Column(nullable = false)
  private LocalDateTime dataHora;

  @Enumerated(EnumType.STRING)
  private SituacaoSolicitacao situacaoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SituacaoSolicitacao situacaoNova;

  @Column(nullable = false, length = 2000)
  private String descricao;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Usuario autor;

  protected EventoHistorico() {}

  public EventoHistorico(
      Solicitacao solicitacao,
      SituacaoSolicitacao anterior,
      SituacaoSolicitacao nova,
      String descricao,
      Usuario autor) {
    this.solicitacao = solicitacao;
    this.situacaoAnterior = anterior;
    this.situacaoNova = nova;
    this.descricao = descricao;
    this.autor = autor;
    this.dataHora = LocalDateTime.now();
  }

  public Solicitacao getSolicitacao() {
    return solicitacao;
  }

  public LocalDateTime getDataHora() {
    return dataHora;
  }

  public SituacaoSolicitacao getSituacaoAnterior() {
    return situacaoAnterior;
  }

  public SituacaoSolicitacao getSituacaoNova() {
    return situacaoNova;
  }

  public String getDescricao() {
    return descricao;
  }

  public Usuario getAutor() {
    return autor;
  }
}
