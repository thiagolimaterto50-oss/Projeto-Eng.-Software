package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Aeronave extends Entidade {
  @Column(nullable = false, length = 12)
  private String matricula;

  @Column(nullable = false, length = 80)
  private String fabricante;

  @Column(nullable = false, length = 80)
  private String modelo;

  @Column(nullable = false)
  private boolean ativa = true;

  @Column(length = 1000)
  private String justificativaVinculo;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Cliente cliente;

  @Column(unique = true, length = 12)
  private String matriculaAtiva;

  @PrePersist
  @PreUpdate
  void atualizarMatriculaAtiva() {
    matricula = matricula.strip().toUpperCase(java.util.Locale.ROOT);
    matriculaAtiva = ativa ? matricula : null;
  }

  protected Aeronave() {}

  public Aeronave(String matricula, String fabricante, String modelo, Cliente cliente) {
    this.matricula = matricula;
    this.fabricante = fabricante;
    this.modelo = modelo;
    this.cliente = cliente;
  }

  public String getMatricula() {
    return matricula;
  }

  public String getFabricante() {
    return fabricante;
  }

  public String getModelo() {
    return modelo;
  }

  public boolean isAtiva() {
    return ativa;
  }

  public String getJustificativaVinculo() {
    return justificativaVinculo;
  }

  public Cliente getCliente() {
    return cliente;
  }
}
