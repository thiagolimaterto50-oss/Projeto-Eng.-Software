package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario extends Entidade {
  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, unique = true, length = 180)
  private String email;

  @Column(nullable = false)
  private String senhaHash;

  @Column(nullable = false)
  private boolean ativo = true;

  protected Usuario() {}

  protected Usuario(String nome, String email, String senhaHash) {
    this.nome = nome;
    this.email = email;
    this.senhaHash = senhaHash;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
