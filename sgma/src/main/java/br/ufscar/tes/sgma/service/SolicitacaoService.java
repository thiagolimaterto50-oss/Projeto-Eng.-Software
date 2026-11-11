package br.ufscar.tes.sgma.service;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class SolicitacaoService {
  private final ISolicitacaoDAO solicitacoes;
  private final IAeronaveDAO aeronaves;
  private final IGestorSolicitacoesDAO gestores;
  private final IResponsavelConfirmacaoDAO responsaveis;

  public SolicitacaoService(
      ISolicitacaoDAO solicitacoes, IAeronaveDAO aeronaves, IGestorSolicitacoesDAO gestores, IResponsavelConfirmacaoDAO responsaveis) {
    this.solicitacoes = solicitacoes;
    this.aeronaves = aeronaves;
    this.gestores = gestores;
    this.responsaveis = responsaveis;
  }

  public record AberturaForm(
      @NotNull Long aeronaveId,
      @NotNull TipoServico tipoServico,
      @NotBlank @Size(max = 2000) String descricao) {}

  public record CancelamentoForm(@NotBlank @Size(max = 1000) String motivo) {}

  public record EncaminhamentoForm(@NotBlank @Size(max=2000) String descricao) {}
  public record ConfirmacaoForm(
      @NotNull @PastOrPresent @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataRealizacao,
      @Size(max=500) String referencia) {}

  public void encaminhar(Long id, @Valid EncaminhamentoForm form) {
    buscar(id).encaminhar(form.descricao(), gestorLocal());
  }

  public void confirmar(Long id, @Valid ConfirmacaoForm form) {
    var responsavel = responsaveis.findFirstByOrderByIdAsc()
        .orElseThrow(() -> new IllegalStateException("Responsável local não configurado."));
    buscar(id).confirmar(form.dataRealizacao(), form.referencia(), responsavel);
  }

  public record PerguntaForm(@NotBlank @Size(max = 2000) String pergunta) {}

  public record RespostaForm(@NotBlank @Size(max = 2000) String resposta) {}

  public record OrcamentoForm(@NotBlank @Size(max = 2000) String descricaoServico,
      @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) java.math.BigDecimal valorTotal,
      @NotNull @FutureOrPresent @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataValidade) {}

  public record DecisaoForm(@NotNull DecisaoOrcamento decisao) {}

  public void enviarOrcamento(Long id, @Valid OrcamentoForm form) {
    buscar(id).enviarOrcamento(form.descricaoServico(), form.valorTotal(), form.dataValidade(), gestorLocal());
  }

  public void iniciarRevisao(Long id, Long orcamentoId) {
    buscar(id).iniciarRevisao(orcamentoId, gestorLocal());
  }

  public void decidirOrcamento(Long id, Long orcamentoId, @Valid DecisaoForm form) {
    Solicitacao solicitacao = buscar(id);
    solicitacao.decidirOrcamento(orcamentoId, form.decisao(), solicitacao.getCliente());
  }

  public void solicitarComplemento(Long id, @Valid PerguntaForm form) {
    buscar(id).solicitarComplemento(form.pergunta(), gestorLocal());
  }

  public void responderComplemento(Long id, Long complementoId, @Valid RespostaForm form) {
    Solicitacao solicitacao = buscar(id);
    // Simulação local do cliente vinculado, até a implementação de autenticação.
    solicitacao.responderComplemento(complementoId, form.resposta(), solicitacao.getCliente());
  }

  public Solicitacao abrir(@Valid AberturaForm form) {
    Aeronave aeronave =
        aeronaves
            .findById(form.aeronaveId())
            .orElseThrow(() -> new IllegalArgumentException("Aeronave não encontrada."));
    return solicitacoes.save(new Solicitacao(aeronave, form.tipoServico(), form.descricao()));
  }

  public void iniciarAnalise(Long id) {
    buscar(id).iniciarAnalise(gestorLocal());
  }

  public void cancelar(Long id, @Valid CancelamentoForm form) {
    buscar(id).cancelar(form.motivo(), gestorLocal());
  }

  private Solicitacao buscar(Long id) {
    return solicitacoes
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada."));
  }

  private GestorSolicitacoes gestorLocal() {
    // Operador explícito da entrega parcial. Não representa uma sessão autenticada.
    return gestores
        .findFirstByOrderByIdAsc()
        .orElseThrow(() -> new IllegalStateException("Operador local não configurado."));
  }
}
