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
      Long clienteId) {}

  public Cliente cadastrarCliente(@Valid ClienteForm form, String senha) {
    acesso.gestor();
    return criarCliente(form, senha);
  }

  public record CadastroPublicoForm(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 180) String email,
      @NotBlank @Size(max = 30) String documento,
      @Size(max = 30) String telefone,
      @NotBlank @Size(min = 8, max = 72) String senha,
      @NotBlank @Size(max = 72) String confirmacaoSenha) {}

  public Cliente cadastrarPublico(@Valid CadastroPublicoForm form) {
    if (!form.senha().equals(form.confirmacaoSenha()))
      throw new IllegalArgumentException("As senhas não coincidem.");
    // O tipo da conta é fixo. Nenhum perfil ou identificador vem do formulário público.
    return criarCliente(new ClienteForm(form.nome(), form.email(), form.documento(), form.telefone()), form.senha());
  }

  private Cliente criarCliente(ClienteForm form, String senha) {
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
    var autor = acesso.atual();
    Long clienteId;
    if (autor instanceof Cliente clienteAtual) {
      if (form.clienteId() != null && !form.clienteId().equals(clienteAtual.getId()))
        throw new org.springframework.security.access.AccessDeniedException("Cadastre a aeronave em sua própria conta.");
      clienteId = clienteAtual.getId();
      justificativa = null;
    } else {
      acesso.gestor();
      clienteId = form.clienteId();
    }
    if (clienteId == null) throw new IllegalArgumentException("Selecione o cliente da aeronave.");
    validarJustificativa(justificativa);
    String matricula = form.matricula().toUpperCase(Locale.ROOT);
    Cliente cliente =
        clientes
            .findById(clienteId)
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
    if (senha != null && !senha.isEmpty()) {
      validarSenha(senha);
      c.definirSenhaHash(encoder.encode(senha));
    }
    atualizarDados(c, form, ativo);
  }

  public void editarMeusDados(@Valid ClienteForm form) {
    var cliente = acesso.cliente();
    atualizarDados(cliente, form, cliente.isAtivo());
  }

  private void atualizarDados(Cliente cliente, ClienteForm form, boolean ativo) {
    String email = form.email().strip().toLowerCase(Locale.ROOT);
    if (usuarios.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(cliente.getId())).isPresent())
      throw new IllegalArgumentException("E-mail já cadastrado.");
    if (clientes.findAll().stream().anyMatch(u ->
        !u.getId().equals(cliente.getId()) && u.getDocumento().equals(form.documento().strip())))
      throw new IllegalArgumentException("Documento já cadastrado.");
    cliente.atualizar(form.nome().strip(), email, form.documento().strip(), form.telefone(), ativo);
  }

  public void editarAeronave(
      Long id, @Valid AeronaveForm form, boolean ativa, String justificativa) {
    var gestor = acesso.gestor();
    validarJustificativa(justificativa);
    var a =
        aeronaves
            .buscarParaAlteracao(id)
            .orElseThrow(() -> new IllegalArgumentException("Aeronave não encontrada."));
    if (form.clienteId() == null) throw new IllegalArgumentException("Selecione o cliente da aeronave.");
    var cliente = clientes.findById(form.clienteId())
        .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    if (ativa && !cliente.isAtivo())
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
    a.corrigirVinculo(cliente, justificativa, gestor);
    a.atualizar(matricula, form.fabricante().strip(), form.modelo().strip(), justificativa, ativa);
  }

  @Transactional(readOnly = true)
  public java.util.List<AlteracaoVinculo> historicoVinculo(Long id) {
    acesso.gestor();
    var historico = aeronaves.findById(id).orElseThrow().getAlteracoesVinculo();
    historico.forEach(h -> {
      h.getClienteAnterior().getNome(); h.getClienteNovo().getNome(); h.getAutor().getNome();
    });
    return java.util.List.copyOf(historico);
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
