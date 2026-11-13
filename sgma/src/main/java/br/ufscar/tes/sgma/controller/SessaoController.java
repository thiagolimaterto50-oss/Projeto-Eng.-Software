package br.ufscar.tes.sgma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import br.ufscar.tes.sgma.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SessaoController {
  private final CadastroService cadastro;
  public SessaoController(CadastroService cadastro) { this.cadastro = cadastro; }

  @GetMapping("/")
  public String inicio(Model model, java.security.Principal principal) {
    model.addAttribute("autenticado", principal != null);
    return "inicio";
  }

  @GetMapping("/cadastro")
  public String cadastro(Model model) {
    if (!model.containsAttribute("form"))
      model.addAttribute("form", new CadastroService.CadastroPublicoForm("", "", "", "", "", ""));
    return "cadastro";
  }

  @PostMapping("/cadastro")
  public String cadastrar(@Valid @ModelAttribute("form") CadastroService.CadastroPublicoForm form,
      BindingResult errors, Model model, RedirectAttributes redirect) {
    if (!errors.hasErrors()) {
      try {
        cadastro.cadastrarPublico(form);
        redirect.addFlashAttribute("sucesso", "Cadastro realizado! Entre com seu e-mail e senha.");
        return "redirect:/login";
      } catch (IllegalArgumentException e) {
        errors.reject("cadastro", e.getMessage());
      } catch (DataIntegrityViolationException e) {
        errors.reject("cadastro", "E-mail ou documento já cadastrado.");
      }
    }
    return cadastro(model);
  }
  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
