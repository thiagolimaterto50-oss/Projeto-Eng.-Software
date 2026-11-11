package br.ufscar.tes.sgma.domain;

public enum TipoServico {
  INSPECAO,
  REPARO,
  MODIFICACAO,
  OUTRO;

  public String getRotulo() {
    return switch (name()) {
      case "INSPECAO" -> "Inspeção";
      case "REPARO" -> "Reparo";
      case "MODIFICACAO" -> "Modificação";
      case "OUTRO" -> "Outro";
      default -> name();
    };
  }
}
