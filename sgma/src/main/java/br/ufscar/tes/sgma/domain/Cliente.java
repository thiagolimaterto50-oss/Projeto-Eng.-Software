package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Cliente extends Usuario {
  @Column(nullable = false, unique = true, length = 30)
  private String documento;

  @Column(length = 30)
  private String telefone;

  protected Cliente() {}

  public Cliente(String nome, String email, String documento, String telefone) {
    super(nome, email, "!SEM_LOGIN");
    this.documento = documento;
    this.telefone = telefone;
  }

  public void atualizar(
      String nome, String email, String documento, String telefone, boolean ativo) {
    atualizarDados(nome, email, ativo);
    this.documento = documento;
    this.telefone = telefone;
  }

  public String getDocumento() {
    return documento;
  }

  public String getTelefone() {
    return telefone;
  }
}
