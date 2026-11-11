package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class Entidade {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  public Long getId() {
    return id;
  }
}
