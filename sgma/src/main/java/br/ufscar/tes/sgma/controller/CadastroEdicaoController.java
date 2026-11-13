package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CadastroEdicaoController {
  private final CadastroService cadastro;
  private final AcessoService acesso;
  private final IClienteDAO clientes;
  private final IAeronaveDAO aeronaves;

  public CadastroEdicaoController(
      CadastroService cadastro,
      AcessoService acesso,
      IClienteDAO clientes,
      IAeronaveDAO aeronaves) {
    this.cadastro = cadastro;
    this.acesso = acesso;
    this.clientes = clientes;
    this.aeronaves = aeronaves;
  }

  @GetMapping("/clientes/{id}/editar")
  public String cliente(@PathVariable Long id, Model model) {
    acesso.gestor();
    var c =
        clientes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!model.containsAttribute("form"))
      model.addAttribute(
          "form",
          new CadastroService.ClienteForm(
              c.getNome(), c.getEmail(), c.getDocumento(), c.getTelefone()));
    model.addAttribute("id", id);
    if (!model.containsAttribute("ativo")) model.addAttribute("ativo", c.isAtivo());
    return "editar-cliente";
  }

  @PostMapping("/clientes/{id}/editar")
  public String clienteSalvar(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") CadastroService.ClienteForm form,
      BindingResult errors,
      @RequestParam(defaultValue = "false") boolean ativo,
      @RequestParam(defaultValue = "") String senha,
      Model model,
      RedirectAttributes redirect) {
    acesso.gestor();
    model.addAttribute("ativo", ativo);
    if (!errors.hasErrors())
      try {
        cadastro.editarCliente(id, form, ativo, senha);
        redirect.addFlashAttribute("sucesso", "Cliente atualizado.");
        return "redirect:/clientes";
      } catch (IllegalArgumentException e) {
        errors.reject("cadastro", e.getMessage());
      }
    return cliente(id, model);
  }

  @GetMapping("/aeronaves/{id}/editar")
  public String aeronave(@PathVariable Long id, Model model) {
    acesso.gestor();
    var a =
        aeronaves.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!model.containsAttribute("form"))
      model.addAttribute(
          "form",
          new CadastroService.AeronaveForm(
              a.getMatricula(), a.getFabricante(), a.getModelo(), a.getCliente().getId()));
    model.addAttribute("id", id);
    if (!model.containsAttribute("ativa")) model.addAttribute("ativa", a.isAtiva());
    if (!model.containsAttribute("justificativaVinculo"))
      model.addAttribute("justificativaVinculo", a.getJustificativaVinculo());
    return "editar-aeronave";
  }

  @PostMapping("/aeronaves/{id}/editar")
  public String aeronaveSalvar(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") CadastroService.AeronaveForm form,
      BindingResult errors,
      @RequestParam(defaultValue = "false") boolean ativa,
      @RequestParam(defaultValue = "") String justificativaVinculo,
      Model model,
      RedirectAttributes redirect) {
    acesso.gestor();
    model.addAttribute("ativa", ativa);
    model.addAttribute("justificativaVinculo", justificativaVinculo);
    if (!errors.hasErrors())
      try {
        cadastro.editarAeronave(id, form, ativa, justificativaVinculo);
        redirect.addFlashAttribute("sucesso", "Aeronave atualizada.");
        return "redirect:/aeronaves";
      } catch (IllegalArgumentException e) {
        errors.reject("cadastro", e.getMessage());
      }
    return aeronave(id, model);
  }
}
