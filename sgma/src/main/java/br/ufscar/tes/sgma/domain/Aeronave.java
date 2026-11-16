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

  @OneToMany(mappedBy = "aeronave", cascade = CascadeType.ALL)
  @OrderBy("dataHora ASC, id ASC")
  private java.util.List<AlteracaoVinculo> alteracoesVinculo = new java.util.ArrayList<>();

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

  public void atualizar(
      String matricula, String fabricante, String modelo, String justificativa, boolean ativa) {
    this.matricula = matricula;
    this.fabricante = fabricante;
    this.modelo = modelo;
    this.justificativaVinculo = justificativa;
    this.ativa = ativa;
  }

  public String getMatricula() {
    return matricula;
  }

  public void corrigirVinculo(Cliente novo, String justificativa, GestorSolicitacoes autor) {
    if (cliente.getId().equals(novo.getId())) return;
    if (justificativa == null || justificativa.isBlank() || justificativa.length() > 1000)
      throw new IllegalArgumentException("Informe uma justificativa de até 1000 caracteres para mudar o cliente.");
    if (autor == null || !autor.isAtivo())
      throw new IllegalArgumentException("Gestor inválido.");
    alteracoesVinculo.add(new AlteracaoVinculo(this, cliente, novo, autor, justificativa.strip()));
    cliente = novo;
  }

  public java.util.List<AlteracaoVinculo> getAlteracoesVinculo() {
    return java.util.Collections.unmodifiableList(alteracoesVinculo);
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
