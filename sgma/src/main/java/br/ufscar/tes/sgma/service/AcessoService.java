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
    var registro = auth.getPrincipal() instanceof br.ufscar.tes.sgma.config.UsuarioAutenticado sessao
        ? usuarios.findById(sessao.getUsuarioId())
        : usuarios.findByEmailIgnoreCase(auth.getName());
    var usuario = org.hibernate.Hibernate.unproxy(
        registro.orElseThrow(() -> new AccessDeniedException("Conta indisponível.")), Usuario.class);
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
    if (u instanceof Cliente) return u.getId();
    if (u instanceof GestorSolicitacoes || u instanceof ResponsavelConfirmacao) return null;
    throw new AccessDeniedException("Perfil indisponível.");
  }

  public void verificarCliente(Long id) {
    Long clienteId = filtroCliente();
    if (clienteId != null && !clienteId.equals(id))
      throw new AccessDeniedException("Registro de outro cliente.");
  }

  public void verificar(Solicitacao s) {
    verificarCliente(s.getCliente().getId());
  }
}
