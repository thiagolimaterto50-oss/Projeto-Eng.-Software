package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class ResponsavelConfirmacao extends Usuario {
  protected ResponsavelConfirmacao() {}

  public ResponsavelConfirmacao(String nome, String email) {
    super(nome, email, "!SEM_LOGIN");
  }
}
