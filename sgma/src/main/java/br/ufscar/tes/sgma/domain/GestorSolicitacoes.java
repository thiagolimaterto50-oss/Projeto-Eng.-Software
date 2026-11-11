package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class GestorSolicitacoes extends Usuario {
  protected GestorSolicitacoes() {}

  public GestorSolicitacoes(String nome, String email) {
    super(nome, email, "!SEM_LOGIN");
  }
}
