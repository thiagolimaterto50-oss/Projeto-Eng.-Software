package br.ufscar.tes.sgma.service;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class CadastroService {
  private final IClienteDAO clientes;
  private final IUsuarioDAO usuarios;
  private final IAeronaveDAO aeronaves;
  private final AcessoService acesso;
  private final org.springframework.security.crypto.password.PasswordEncoder encoder;

  public CadastroService(
      IClienteDAO clientes,
      IUsuarioDAO usuarios,
      IAeronaveDAO aeronaves,
      AcessoService acesso,
      org.springframework.security.crypto.password.PasswordEncoder encoder) {
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.aeronaves = aeronaves;
    this.acesso = acesso;
    this.encoder = encoder;
  }

  public record ClienteForm(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 180) String email,
      @NotBlank @Size(max = 30) String documento,
      @Size(max = 30) String telefone) {}

  public record AeronaveForm(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{3,12}") String matricula,
      @NotBlank @Size(max = 80) String fabricante,
      @NotBlank @Size(max = 80) String modelo,
      @NotNull Long clienteId) {}

  public Cliente cadastrarCliente(@Valid ClienteForm form, String senha) {
    acesso.gestor();
    validarSenha(senha);
    String email = form.email().strip().toLowerCase(Locale.ROOT);
    if (usuarios.existsByEmailIgnoreCase(email))
      throw new IllegalArgumentException("E-mail já cadastrado.");
    if (clientes.existsByDocumento(form.documento().strip()))
      throw new IllegalArgumentException("Documento já cadastrado.");
    var cliente =
        new Cliente(form.nome().strip(), email, form.documento().strip(), form.telefone());
    cliente.definirSenhaHash(encoder.encode(senha));
    return clientes.save(cliente);
  }

  public Aeronave cadastrarAeronave(@Valid AeronaveForm form, String justificativa) {
    acesso.gestor();
    validarJustificativa(justificativa);
    String matricula = form.matricula().toUpperCase(Locale.ROOT);
    Cliente cliente =
        clientes
            .findById(form.clienteId())
            .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    if (!cliente.isAtivo()) throw new IllegalArgumentException("Cliente inativo.");
    if (aeronaves.existsByMatriculaIgnoreCaseAndAtivaTrue(matricula))
      throw new IllegalArgumentException("Já existe uma aeronave ativa com esta matrícula.");
    var a = new Aeronave(matricula, form.fabricante().strip(), form.modelo().strip(), cliente);
    a.atualizar(matricula, form.fabricante().strip(), form.modelo().strip(), justificativa, true);
    return aeronaves.save(a);
  }

  public void editarCliente(Long id, @Valid ClienteForm form, boolean ativo, String senha) {
    acesso.gestor();
    var c =
        clientes
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    String email = form.email().strip().toLowerCase(Locale.ROOT);
    if (usuarios.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(id)).isPresent())
      throw new IllegalArgumentException("E-mail já cadastrado.");
    if (clientes.findAll().stream()
        .anyMatch(u -> !u.getId().equals(id) && u.getDocumento().equals(form.documento().strip())))
      throw new IllegalArgumentException("Documento já cadastrado.");
    if (senha != null && !senha.isEmpty()) {
      validarSenha(senha);
      c.definirSenhaHash(encoder.encode(senha));
    }
    c.atualizar(form.nome().strip(), email, form.documento().strip(), form.telefone(), ativo);
  }

  public void editarAeronave(
      Long id, @Valid AeronaveForm form, boolean ativa, String justificativa) {
    acesso.gestor();
    validarJustificativa(justificativa);
    var a =
        aeronaves
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aeronave não encontrada."));
    // Preserva o proprietário para manter consistência com as solicitações históricas.
    if (!a.getCliente().getId().equals(form.clienteId()))
      throw new IllegalArgumentException("O proprietário não pode ser alterado neste cadastro.");
    if (ativa && !a.getCliente().isAtivo())
      throw new IllegalArgumentException("Reative o cliente antes de ativar a aeronave.");
    String matricula = form.matricula().toUpperCase(Locale.ROOT);
    if (ativa
        && aeronaves.findAll().stream()
            .anyMatch(
                o ->
                    !o.getId().equals(id)
                        && o.isAtiva()
                        && o.getMatricula().equalsIgnoreCase(matricula)))
      throw new IllegalArgumentException("Já existe uma aeronave ativa com esta matrícula.");
    a.atualizar(matricula, form.fabricante().strip(), form.modelo().strip(), justificativa, ativa);
  }

  private void validarJustificativa(String texto) {
    if (texto != null && texto.length() > 1000)
      throw new IllegalArgumentException("Justificativa deve ter até 1000 caracteres.");
  }

  private void validarSenha(String senha) {
    if (senha == null
        || senha.isBlank()
        || senha.length() < 8
        || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Senha deve ter no mínimo 8 caracteres e no máximo 72 bytes.");
  }
}
