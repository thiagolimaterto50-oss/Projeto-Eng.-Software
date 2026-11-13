package br.ufscar.tes.sgma.service;

import br.ufscar.tes.sgma.dao.IUsuarioDAO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContaService {
  private final AcessoService acesso;
  private final PasswordEncoder encoder;
  private final IUsuarioDAO usuarios;

  public ContaService(AcessoService acesso, PasswordEncoder encoder, IUsuarioDAO usuarios) {
    this.acesso = acesso;
    this.encoder = encoder;
    this.usuarios = usuarios;
  }

  public void alterarSenha(String atual, String nova) {
    var u = acesso.atual();
    if (!encoder.matches(atual, u.getSenhaHash()))
      throw new IllegalArgumentException("Senha atual incorreta.");
    if (nova == null
        || nova.isBlank()
        || nova.length() < 8
        || nova.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Nova senha deve ter no mínimo 8 caracteres e no máximo 72 bytes.");
    u.definirSenhaHash(encoder.encode(nova));
    usuarios.save(u);
  }
}
