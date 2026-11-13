package br.ufscar.tes.sgma.config;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DadosIniciais {
  @Bean
  CommandLineRunner contasIniciais(
      IUsuarioDAO usuarios,
      IClienteDAO clientes,
      IAeronaveDAO aeronaves,
      PasswordEncoder encoder,
      Environment env) {
    return args -> {
      boolean demo = env.acceptsProfiles(Profiles.of("demo", "test"));
      String gestorEmail =
          env.getProperty("SGMA_GESTOR_EMAIL", demo ? "operador@sgma.invalid" : "");
      String gestorSenha = env.getProperty("SGMA_GESTOR_SENHA", demo ? "Gestor123!" : "");
      String responsavelEmail =
          env.getProperty("SGMA_RESPONSAVEL_EMAIL", demo ? "confirmacao@sgma.invalid" : "");
      String responsavelSenha =
          env.getProperty("SGMA_RESPONSAVEL_SENHA", demo ? "Responsavel123!" : "");
      preparar(usuarios, encoder, new GestorSolicitacoes("Gestor", gestorEmail), gestorSenha);
      preparar(
          usuarios,
          encoder,
          new ResponsavelConfirmacao("Responsável", responsavelEmail),
          responsavelSenha);
      if (demo) {
        preparar(
            usuarios,
            encoder,
            new Cliente("Cliente demonstração", "cliente@sgma.invalid", "DEMO-LOGIN", ""),
            "Cliente123!");
        var c = (Cliente) usuarios.findByEmailIgnoreCase("cliente@sgma.invalid").orElseThrow();
        if (aeronaves.findAll().stream()
            .noneMatch(a -> a.getMatricula().equalsIgnoreCase("PT-LOGIN")))
          aeronaves.save(new Aeronave("PT-LOGIN", "Embraer", "Demonstração", c));
      }
    };
  }

  private void preparar(IUsuarioDAO usuarios, PasswordEncoder encoder, Usuario novo, String senha) {
    if (novo.getEmail().isBlank() || senha.isBlank()) return;
    var existente = usuarios.findByEmailIgnoreCase(novo.getEmail());
    // Migra somente os marcadores antigos. Nunca redefine uma senha já configurada.
    if (existente.isPresent() && !existente.get().getSenhaHash().equals("!SEM_LOGIN")) return;
    if (senha.length() < 8 || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Senha inicial deve ter no mínimo 8 caracteres e no máximo 72 bytes.");
    var u = existente.orElse(novo);
    u.definirSenhaHash(encoder.encode(senha));
    usuarios.save(u);
  }
}
