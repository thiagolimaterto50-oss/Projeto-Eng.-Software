package br.ufscar.tes.sgma.service;

import br.ufscar.tes.sgma.dao.IUsuarioDAO;
import br.ufscar.tes.sgma.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AcessoService {
  private final IUsuarioDAO usuarios;

  public AcessoService(IUsuarioDAO usuarios) {
    this.usuarios = usuarios;
  }

  public Usuario atual() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated())
      throw new AccessDeniedException("Entre para continuar.");
    var usuario =
        usuarios
            .findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("Conta indisponível."));
    if (!usuario.isAtivo()) throw new AccessDeniedException("Conta inativa.");
    return usuario;
  }

  public GestorSolicitacoes gestor() {
    var u = atual();
    if (!(u instanceof GestorSolicitacoes g))
      throw new AccessDeniedException("Ação exclusiva do gestor.");
    return g;
  }

  public Cliente cliente() {
    var u = atual();
    if (!(u instanceof Cliente c)) throw new AccessDeniedException("Ação exclusiva do cliente.");
    return c;
  }

  public ResponsavelConfirmacao responsavel() {
    var u = atual();
    if (!(u instanceof ResponsavelConfirmacao r))
      throw new AccessDeniedException("Ação exclusiva do responsável.");
    return r;
  }

  public Long filtroCliente() {
    var u = atual();
    return u instanceof Cliente ? u.getId() : null;
  }

  public void verificarCliente(Long id) {
    var u = atual();
    if (u instanceof Cliente && !u.getId().equals(id))
      throw new AccessDeniedException("Registro de outro cliente.");
  }

  public void verificar(Solicitacao s) {
    verificarCliente(s.getCliente().getId());
  }
}
