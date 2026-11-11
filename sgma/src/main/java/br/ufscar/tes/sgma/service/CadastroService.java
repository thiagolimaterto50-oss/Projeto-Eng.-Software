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

  public CadastroService(IClienteDAO clientes, IUsuarioDAO usuarios, IAeronaveDAO aeronaves) {
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.aeronaves = aeronaves;
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

  public Cliente cadastrarCliente(@Valid ClienteForm form) {
    String email = form.email().strip().toLowerCase(Locale.ROOT);
    if (usuarios.existsByEmailIgnoreCase(email))
      throw new IllegalArgumentException("E-mail já cadastrado.");
    if (clientes.existsByDocumento(form.documento().strip()))
      throw new IllegalArgumentException("Documento já cadastrado.");
    return clientes.save(
        new Cliente(form.nome().strip(), email, form.documento().strip(), form.telefone()));
  }

  public Aeronave cadastrarAeronave(@Valid AeronaveForm form) {
    String matricula = form.matricula().toUpperCase(Locale.ROOT);
    Cliente cliente =
        clientes
            .findById(form.clienteId())
            .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    if (!cliente.isAtivo()) throw new IllegalArgumentException("Cliente inativo.");
    if (aeronaves.existsByMatriculaIgnoreCaseAndAtivaTrue(matricula))
      throw new IllegalArgumentException("Já existe uma aeronave ativa com esta matrícula.");
    return aeronaves.save(
        new Aeronave(matricula, form.fabricante().strip(), form.modelo().strip(), cliente));
  }
}
