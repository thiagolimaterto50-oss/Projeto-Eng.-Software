package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Complemento extends Entidade {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Solicitacao solicitacao;

  @Column(nullable = false, length = 2000)
  private String pergunta;

  @Column(nullable = false)
  private LocalDateTime dataHoraPergunta;

  @Column(length = 2000)
  private String resposta;

  private LocalDateTime dataHoraResposta;

  protected Complemento() {}

  public Complemento(Solicitacao solicitacao, String pergunta) {
    if (pergunta == null || pergunta.isBlank() || pergunta.length() > 2000)
      throw new IllegalArgumentException("Informe uma pergunta de até 2000 caracteres.");
    this.solicitacao = solicitacao;
    this.pergunta = pergunta.strip();
    this.dataHoraPergunta = LocalDateTime.now();
  }

  public void responder(String resposta) {
    if (resposta == null || resposta.isBlank() || resposta.length() > 2000)
      throw new IllegalArgumentException("Informe uma resposta de até 2000 caracteres.");
    if (this.resposta != null) throw new IllegalStateException("Complemento já respondido.");
    this.resposta = resposta.strip();
    this.dataHoraResposta = LocalDateTime.now();
  }

  public Solicitacao getSolicitacao() {
    return solicitacao;
  }

  public String getPergunta() {
    return pergunta;
  }

  public LocalDateTime getDataHoraPergunta() {
    return dataHoraPergunta;
  }

  public String getResposta() {
    return resposta;
  }

  public LocalDateTime getDataHoraResposta() {
    return dataHoraResposta;
  }
}
