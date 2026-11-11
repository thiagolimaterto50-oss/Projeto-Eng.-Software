package br.ufscar.tes.sgma.domain;

public enum SituacaoSolicitacao {
  ABERTA,
  EM_ANALISE,
  AGUARDANDO_INFORMACAO,
  ORCAMENTO_ENVIADO,
  APROVADA,
  ENCAMINHADA,
  CONCLUIDA,
  RECUSADA,
  CANCELADA;

  public String getRotulo() {
    return switch (name()) {
      case "ABERTA" -> "Aberta";
      case "EM_ANALISE" -> "Em análise";
      case "AGUARDANDO_INFORMACAO" -> "Aguardando informação";
      case "ORCAMENTO_ENVIADO" -> "Orçamento enviado";
      case "APROVADA" -> "Aprovada";
      case "ENCAMINHADA" -> "Encaminhada";
      case "CONCLUIDA" -> "Concluída";
      case "RECUSADA" -> "Recusada";
      case "CANCELADA" -> "Cancelada";
      default -> name();
    };
  }
}
