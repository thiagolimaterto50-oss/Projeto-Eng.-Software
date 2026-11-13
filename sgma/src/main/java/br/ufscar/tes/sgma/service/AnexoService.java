package br.ufscar.tes.sgma.service;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AnexoService {
  private final ISolicitacaoDAO solicitacoes;
  private final IAnexoDAO anexos;
  private final AcessoService acesso;

  public AnexoService(ISolicitacaoDAO s, IAnexoDAO a, AcessoService acesso) {
    solicitacoes = s;
    anexos = a;
    this.acesso = acesso;
  }

  private Solicitacao buscar(Long id) {
    var s =
        solicitacoes
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    acesso.verificar(s);
    return s;
  }

  public void salvar(Long id, MultipartFile arquivo) {
    var autor = acesso.atual();
    if (!(autor instanceof Cliente) && !(autor instanceof GestorSolicitacoes))
      throw new AccessDeniedException("Sem permissão para anexar.");
    var s = buscar(id);
    if (arquivo == null || arquivo.isEmpty() || arquivo.getSize() > 5 * 1024 * 1024)
      throw new IllegalArgumentException("Selecione um PDF, PNG ou JPEG de até 5 MB.");
    String nome = arquivo.getOriginalFilename();
    if (nome == null) throw new IllegalArgumentException("Arquivo sem nome.");
    nome = nome.replace((char) 92, '_').replace('/', '_').replaceAll("\\p{Cntrl}", "_");
    if (nome.length() > 180)
      throw new IllegalArgumentException("Nome do arquivo deve ter até 180 caracteres.");
    byte[] bytes;
    try {
      bytes = arquivo.getBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Não foi possível ler o arquivo.");
    }
    String lower = nome.toLowerCase(Locale.ROOT);
    String tipo;
    if (lower.endsWith(".pdf")
        && bytes.length >= 5
        && bytes[0] == '%'
        && bytes[1] == 'P'
        && bytes[2] == 'D'
        && bytes[3] == 'F'
        && bytes[4] == '-') tipo = "application/pdf";
    else if (lower.endsWith(".png")
        && bytes.length >= 8
        && bytes[0] == (byte) 137
        && bytes[1] == 80
        && bytes[2] == 78
        && bytes[3] == 71
        && bytes[4] == 13
        && bytes[5] == 10
        && bytes[6] == 26
        && bytes[7] == 10) tipo = "image/png";
    else if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        && bytes.length >= 3
        && bytes[0] == (byte) 255
        && bytes[1] == (byte) 216
        && bytes[2] == (byte) 255) tipo = "image/jpeg";
    else throw new IllegalArgumentException("Formato inválido. Envie PDF, PNG ou JPEG.");
    s.registrarAnexo(nome, autor);
    var anexo = anexos.findBySolicitacaoId(id).orElseGet(() -> new Anexo(s));
    anexo.atualizar(nome, tipo, bytes);
    anexos.save(anexo);
  }

  @Transactional(readOnly = true)
  public Anexo baixar(Long id) {
    buscar(id);
    return anexos
        .findBySolicitacaoId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
