package br.ufscar.tes.sgma.config;

import br.ufscar.tes.sgma.dao.IGestorSolicitacoesDAO;
import br.ufscar.tes.sgma.domain.GestorSolicitacoes;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DadosIniciais {
  @Bean
  CommandLineRunner operadorLocal(IGestorSolicitacoesDAO gestores, br.ufscar.tes.sgma.dao.IResponsavelConfirmacaoDAO responsaveis) {
    return args -> {
      if (gestores.count() == 0)
        gestores.save(new GestorSolicitacoes("Operador local", "operador@sgma.invalid"));
      if (responsaveis.count() == 0)
        responsaveis.save(new br.ufscar.tes.sgma.domain.ResponsavelConfirmacao("Responsável local", "confirmacao@sgma.invalid"));
    };
  }
}
