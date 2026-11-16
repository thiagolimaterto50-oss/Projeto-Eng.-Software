package br.ufscar.tes.sgma.config;

import br.ufscar.tes.sgma.domain.Usuario;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Identidade estável da sessão, inclusive depois de alterar o e-mail de acesso. */
public class UsuarioAutenticado extends User {
  private static final long serialVersionUID = 1L;
  private final Long usuarioId;

  public UsuarioAutenticado(Usuario usuario) {
    super(usuario.getEmail(), usuario.getSenhaHash(), usuario.isAtivo(), true, true, true,
        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil())));
    this.usuarioId = usuario.getId();
  }

  public Long getUsuarioId() {
    return usuarioId;
  }
}
