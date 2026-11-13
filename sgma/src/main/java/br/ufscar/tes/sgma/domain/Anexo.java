package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;

@Entity
public class Anexo extends Entidade {
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private Solicitacao solicitacao;

  @Column(nullable = false, length = 180)
  private String nome;

  @Column(nullable = false, length = 80)
  private String tipo;

  @Lob
  @Column(nullable = false, columnDefinition = "LONGBLOB")
  private byte[] conteudo;

  protected Anexo() {}

  public Anexo(Solicitacao solicitacao) {
    this.solicitacao = solicitacao;
  }

  public void atualizar(String nome, String tipo, byte[] conteudo) {
    this.nome = nome;
    this.tipo = tipo;
    this.conteudo = conteudo;
  }

  public String getNome() {
    return nome;
  }

  public String getTipo() {
    return tipo;
  }

  public byte[] getConteudo() {
    return conteudo;
  }
}
