package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContaController {
  private final ContaService service;

  private final CadastroService cadastro;
  private final AcessoService acesso;

  public ContaController(ContaService service, CadastroService cadastro, AcessoService acesso) {
    this.cadastro = cadastro;
    this.acesso = acesso;
    this.service = service;
  }

  @GetMapping("/conta")
  public String conta(org.springframework.ui.Model model) {
    if (acesso.atual() instanceof br.ufscar.tes.sgma.domain.Cliente c && !model.containsAttribute("form"))
      model.addAttribute("form", new CadastroService.ClienteForm(c.getNome(), c.getEmail(), c.getDocumento(), c.getTelefone()));
    return "conta";
  }

  @PostMapping("/conta/dados")
  public String dados(@jakarta.validation.Valid @ModelAttribute("form") CadastroService.ClienteForm form,
      org.springframework.validation.BindingResult errors, org.springframework.ui.Model model,
      RedirectAttributes redirect) {
    acesso.cliente();
    if (!errors.hasErrors()) {
      try {
        cadastro.editarMeusDados(form);
        redirect.addFlashAttribute("sucesso", "Dados atualizados. Use o novo e-mail no próximo login.");
        return "redirect:/conta";
      } catch (IllegalArgumentException e) {
        errors.reject("cadastro", e.getMessage());
      }
    }
    return conta(model);
  }

  @PostMapping("/conta/senha")
  public String senha(
      @RequestParam String senhaAtual,
      @RequestParam String novaSenha,
      RedirectAttributes redirect) {
    try {
      service.alterarSenha(senhaAtual, novaSenha);
      redirect.addFlashAttribute("sucesso", "Senha alterada.");
    } catch (IllegalArgumentException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/conta";
  }
}
