package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContaController {
  private final ContaService service;

  public ContaController(ContaService service) {
    this.service = service;
  }

  @GetMapping("/conta")
  public String conta() {
    return "conta";
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
