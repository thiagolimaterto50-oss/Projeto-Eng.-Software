package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Confirmacao extends Entidade {
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private Solicitacao solicitacao;

  @Column(nullable = false)
  private LocalDate dataRealizacaoInformada;

  @Column(nullable = false)
  private LocalDateTime dataHoraRegistro;

  @Column(length = 500)
  private String referencia;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private ResponsavelConfirmacao responsavel;

  protected Confirmacao() {}

  public Confirmacao(
      Solicitacao solicitacao,
      LocalDate data,
      String referencia,
      ResponsavelConfirmacao responsavel) {
    if (data == null || data.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("Informe uma data de realização até hoje.");
    if (referencia != null && referencia.length() > 500)
      throw new IllegalArgumentException("A referência deve ter até 500 caracteres.");
    if (responsavel == null || !responsavel.isAtivo())
      throw new IllegalArgumentException("Responsável inválido.");
    this.solicitacao = solicitacao;
    this.dataRealizacaoInformada = data;
    this.referencia = referencia;
    this.responsavel = responsavel;
    this.dataHoraRegistro = LocalDateTime.now();
  }

  public Solicitacao getSolicitacao() {
    return solicitacao;
  }

  public LocalDate getDataRealizacaoInformada() {
    return dataRealizacaoInformada;
  }

  public LocalDateTime getDataHoraRegistro() {
    return dataHoraRegistro;
  }

  public String getReferencia() {
    return referencia;
  }

  public ResponsavelConfirmacao getResponsavel() {
    return responsavel;
  }
}
