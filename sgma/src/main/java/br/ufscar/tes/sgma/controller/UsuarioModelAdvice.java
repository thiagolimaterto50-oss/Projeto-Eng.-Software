package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.domain.*;
import br.ufscar.tes.sgma.service.AcessoService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(
    assignableTypes = {
      PortalController.class,
      CadastroEdicaoController.class,
      AnexoController.class,
      ContaController.class
    })
public class UsuarioModelAdvice {
  private final AcessoService acesso;

  public UsuarioModelAdvice(AcessoService acesso) {
    this.acesso = acesso;
  }

  @ModelAttribute
  public void usuario(Model model) {
    var u = acesso.atual();
    model.addAttribute("usuarioAtual", u);
    model.addAttribute("gestor", u instanceof GestorSolicitacoes);
    model.addAttribute("cliente", u instanceof Cliente);
    model.addAttribute("responsavel", u instanceof ResponsavelConfirmacao);
  }
}
