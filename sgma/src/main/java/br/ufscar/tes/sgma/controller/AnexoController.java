package br.ufscar.tes.sgma.controller;

import br.ufscar.tes.sgma.service.AnexoService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AnexoController {
  private final AnexoService service;

  public AnexoController(AnexoService service) {
    this.service = service;
  }

  @PostMapping("/solicitacoes/{id}/anexo")
  public String enviar(
      @PathVariable Long id, @RequestParam MultipartFile arquivo, RedirectAttributes redirect) {
    try {
      service.salvar(id, arquivo);
      redirect.addFlashAttribute("sucesso", "Anexo salvo.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/solicitacoes/" + id;
  }

  @GetMapping("/solicitacoes/{id}/anexo")
  public ResponseEntity<byte[]> baixar(@PathVariable Long id) {
    var a = service.baixar(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(a.getTipo()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(a.getNome(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(a.getConteudo());
  }
}
