package br.ufscar.tes.sgma.domain;

public enum DecisaoOrcamento {
  PENDENTE,
  APROVADO,
  RECUSADO;

  public String getRotulo() {
    return switch (name()) {
      case "PENDENTE" -> "Pendente";
      case "APROVADO" -> "Aprovado";
      case "RECUSADO" -> "Recusado";
      default -> name();
    };
  }
}
