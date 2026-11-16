package br.ufscar.tes.sgma.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AlteracaoVinculo extends Entidade {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Aeronave aeronave;
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Cliente clienteAnterior;
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Cliente clienteNovo;
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private GestorSolicitacoes autor;
  @Column(nullable = false, length = 1000)
  private String justificativa;
  @Column(nullable = false)
  private LocalDateTime dataHora;

  protected AlteracaoVinculo() {}

  public AlteracaoVinculo(Aeronave aeronave, Cliente anterior, Cliente novo,
      GestorSolicitacoes autor, String justificativa) {
    this.aeronave = aeronave;
    this.clienteAnterior = anterior;
    this.clienteNovo = novo;
    this.autor = autor;
    this.justificativa = justificativa;
    this.dataHora = LocalDateTime.now();
  }

  public Cliente getClienteAnterior() { return clienteAnterior; }
  public Cliente getClienteNovo() { return clienteNovo; }
  public GestorSolicitacoes getAutor() { return autor; }
  public String getJustificativa() { return justificativa; }
  public LocalDateTime getDataHora() { return dataHora; }
}
