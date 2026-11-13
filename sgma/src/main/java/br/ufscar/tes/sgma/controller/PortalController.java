package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import br.ufscar.tes.sgma.service.*;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PortalController {
  private final IClienteDAO clientes;
  private final IAeronaveDAO aeronaves;
  private final ISolicitacaoDAO solicitacoes;
  private final CadastroService cadastro;
  private final SolicitacaoService servico;
  private final AcessoService acesso;

  public PortalController(
      IClienteDAO clientes,
      IAeronaveDAO aeronaves,
      ISolicitacaoDAO solicitacoes,
      CadastroService cadastro,
      SolicitacaoService servico,
      AcessoService acesso) {
    this.clientes = clientes;
    this.aeronaves = aeronaves;
    this.solicitacoes = solicitacoes;
    this.cadastro = cadastro;
    this.servico = servico;
    this.acesso = acesso;
  }

  @ModelAttribute
  void referencias(Model model) {
    model.addAttribute("situacoes", SituacaoSolicitacao.values());
    model.addAttribute("tipos", TipoServico.values());
  }

  @GetMapping("/painel")
  @Transactional(readOnly = true)
  public String painel(Model model) {
    model.addAttribute("total", solicitacoes.contar(acesso.filtroCliente(), null));
    model.addAttribute(
        "abertas", solicitacoes.contar(acesso.filtroCliente(), SituacaoSolicitacao.ABERTA));
    model.addAttribute(
        "analise", solicitacoes.contar(acesso.filtroCliente(), SituacaoSolicitacao.EM_ANALISE));
    model.addAttribute("aeronavesTotal", aeronavesVisiveis().size());
    var recentes =
        solicitacoes
            .pesquisar(
                null, "%", acesso.filtroCliente(), PageRequest.of(0, 5, Sort.by("id").descending()))
            .getContent();
    // Materializa os vínculos ainda na transação, sem Open Session in View.
    recentes.forEach(
        s -> {
          s.getCliente().getNome();
          s.getAeronave().getMatricula();
        });
    model.addAttribute("registros", recentes);
    return "painel";
  }

  @GetMapping("/clientes")
  public String clientes(Model model) {
    model.addAttribute("clientes", clientes.findAll());
    if (!model.containsAttribute("form"))
      model.addAttribute("form", new CadastroService.ClienteForm("", "", "", ""));
    return "clientes";
  }

  @PostMapping("/clientes")
  public String cadastrarCliente(
      @Valid @ModelAttribute("form") CadastroService.ClienteForm form,
      @RequestParam(defaultValue = "") String senha,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) return clientes(model);
    try {
      cadastro.cadastrarCliente(form, senha);
    } catch (IllegalArgumentException e) {
      errors.reject("cadastro", e.getMessage());
      return clientes(model);
    }
    redirect.addFlashAttribute("sucesso", "Cliente cadastrado.");
    return "redirect:/clientes";
  }

  @GetMapping("/aeronaves")
  @Transactional(readOnly = true)
  public String aeronaves(Model model) {
    var lista = aeronavesVisiveis();
    lista.forEach(a -> a.getCliente().getNome());
    model.addAttribute("aeronaves", lista);
    model.addAttribute(
        "clientes",
        acesso.atual() instanceof GestorSolicitacoes
            ? clientes.findAll().stream().filter(Cliente::isAtivo).toList()
            : java.util.List.of());
    if (!model.containsAttribute("form"))
      model.addAttribute("form", new CadastroService.AeronaveForm("", "", "", null));
    return "aeronaves";
  }

  @PostMapping("/aeronaves")
  public String cadastrarAeronave(
      @Valid @ModelAttribute("form") CadastroService.AeronaveForm form,
      @RequestParam(defaultValue = "") String justificativaVinculo,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) return aeronaves(model);
    try {
      cadastro.cadastrarAeronave(form, justificativaVinculo);
    } catch (IllegalArgumentException e) {
      errors.reject("cadastro", e.getMessage());
      return aeronaves(model);
    }
    redirect.addFlashAttribute("sucesso", "Aeronave cadastrada.");
    return "redirect:/aeronaves";
  }

  @GetMapping("/solicitacoes")
  @Transactional(readOnly = true)
  public String listar(
      @RequestParam(defaultValue = "") String busca,
      @RequestParam(required = false) SituacaoSolicitacao situacao,
      @RequestParam(defaultValue = "0") int pagina,
      Model model) {
    String termo =
        busca
            .strip()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    var resultado =
        solicitacoes.pesquisar(
            situacao,
            "%" + termo + "%",
            acesso.filtroCliente(),
            PageRequest.of(Math.max(0, pagina), 10, Sort.by("id").descending()));
    resultado.forEach(
        s -> {
          s.getCliente().getNome();
          s.getAeronave().getMatricula();
        });
    model.addAttribute("resultado", resultado);
    model.addAttribute("registros", resultado.getContent());
    model.addAttribute("busca", busca);
    model.addAttribute("situacaoSelecionada", situacao);
    return "solicitacoes";
  }

  @GetMapping("/solicitacoes/nova")
  @Transactional(readOnly = true)
  public String nova(Model model) {
    acesso.cliente();
    var lista =
        aeronavesVisiveis().stream().filter(a -> a.isAtiva() && a.getCliente().isAtivo()).toList();
    lista.forEach(a -> a.getCliente().getNome());
    model.addAttribute("aeronaves", lista);
    if (!model.containsAttribute("form"))
      model.addAttribute("form", new SolicitacaoService.AberturaForm(null, null, ""));
    return "nova-solicitacao";
  }

  @PostMapping("/solicitacoes")
  public String abrir(
      @Valid @ModelAttribute("form") SolicitacaoService.AberturaForm form,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) return nova(model);
    try {
      Long id = servico.abrir(form).getId();
      redirect.addFlashAttribute("sucesso", "Solicitação aberta e registrada no histórico.");
      return "redirect:/solicitacoes/" + id;
    } catch (IllegalArgumentException e) {
      errors.reject("abertura", e.getMessage());
      return nova(model);
    }
  }

  @GetMapping("/solicitacoes/{id}")
  @Transactional(readOnly = true)
  public String detalhes(@PathVariable Long id, Model model) {
    var s =
        solicitacoes
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    acesso.verificar(s);
    s.getCliente().getNome();
    s.getAeronave().getMatricula();
    s.getHistorico().forEach(e -> e.getAutor().getNome());
    s.getComplementos().size();
    if (s.getConfirmacao() != null) s.getConfirmacao().getResponsavel().getNome();
    s.getOrcamentos()
        .forEach(
            o -> {
              o.getElaborador().getNome();
              if (o.getDecisor() != null) o.getDecisor().getNome();
            });
    model.addAttribute("solicitacao", s);
    return "detalhes";
  }

  @PostMapping("/solicitacoes/{id}/complementos")
  public String pedirComplemento(
      @PathVariable Long id,
      @Valid SolicitacaoService.PerguntaForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute("erro", "Informe uma pergunta de até 2000 caracteres.");
    else
      try {
        servico.solicitarComplemento(id, form);
        redirect.addFlashAttribute(
            "sucesso", "Pergunta registrada. Aguardando informação do cliente.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/complementos/{complementoId}/responder")
  public String responderComplemento(
      @PathVariable Long id,
      @PathVariable Long complementoId,
      @Valid SolicitacaoService.RespostaForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute("erro", "Informe uma resposta de até 2000 caracteres.");
    else
      try {
        servico.responderComplemento(id, complementoId, form);
        redirect.addFlashAttribute(
            "sucesso", "Resposta registrada. Solicitação voltou para análise.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/analisar")
  public String analisar(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      servico.iniciarAnalise(id);
      redirect.addFlashAttribute("sucesso", "Análise iniciada.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/orcamentos")
  public String enviarOrcamento(
      @PathVariable Long id,
      @Valid SolicitacaoService.OrcamentoForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute(
          "erro",
          "Confira a descrição, o valor positivo (até duas casas decimais) e a validade a partir de"
              + " hoje.");
    else
      try {
        servico.enviarOrcamento(id, form);
        redirect.addFlashAttribute("sucesso", "Orçamento enviado para decisão do cliente.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/orcamentos/{oid}/decidir")
  public String decidirOrcamento(
      @PathVariable Long id,
      @PathVariable Long oid,
      @Valid SolicitacaoService.DecisaoForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) redirect.addFlashAttribute("erro", "Selecione aprovar ou recusar.");
    else
      try {
        servico.decidirOrcamento(id, oid, form);
        redirect.addFlashAttribute("sucesso", "Decisão registrada no histórico.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/orcamentos/{oid}/revisar")
  public String revisarOrcamento(
      @PathVariable Long id, @PathVariable Long oid, RedirectAttributes redirect) {
    try {
      servico.iniciarRevisao(id, oid);
      redirect.addFlashAttribute(
          "sucesso", "Revisão iniciada. Envie uma nova versão após a análise.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/cancelar")
  public String cancelar(
      @PathVariable Long id,
      @Valid SolicitacaoService.CancelamentoForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute("erro", "Informe um motivo de até 1000 caracteres.");
    else
      try {
        servico.cancelar(id, form);
        redirect.addFlashAttribute("sucesso", "Solicitação cancelada.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/encaminhar")
  public String encaminhar(
      @PathVariable Long id,
      @Valid SolicitacaoService.EncaminhamentoForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute("erro", "Informe o encaminhamento, até 2000 caracteres.");
    else
      try {
        servico.encaminhar(id, form);
        redirect.addFlashAttribute(
            "sucesso", "Serviço encaminhado. Aguardando confirmação da realização.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  @PostMapping("/solicitacoes/{id}/confirmar")
  public String confirmar(
      @PathVariable Long id,
      @Valid SolicitacaoService.ConfirmacaoForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute(
          "erro", "Informe a data de realização até hoje e uma referência de até 500 caracteres.");
    else
      try {
        servico.confirmar(id, form);
        redirect.addFlashAttribute("sucesso", "Realização confirmada. Solicitação concluída.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }

  private java.util.List<Aeronave> aeronavesVisiveis() {
    Long clienteId = acesso.filtroCliente();
    return aeronaves.findAll().stream()
        .filter(a -> clienteId == null || a.getCliente().getId().equals(clienteId))
        .toList();
  }

  @PostMapping("/solicitacoes/{id}/triagem")
  public String triagem(
      @PathVariable Long id,
      @Valid SolicitacaoService.TriagemForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors())
      redirect.addFlashAttribute("erro", "Informe observações de até 2000 caracteres.");
    else
      try {
        servico.registrarTriagem(id, form);
        redirect.addFlashAttribute("sucesso", "Triagem registrada.");
      } catch (IllegalArgumentException | IllegalStateException e) {
        redirect.addFlashAttribute("erro", e.getMessage());
      }
    return "redirect:/solicitacoes/" + id;
  }
}
