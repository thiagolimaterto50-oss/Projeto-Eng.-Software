package br.ufscar.tes.sgma.config;

import br.ufscar.tes.sgma.dao.IUsuarioDAO;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(IUsuarioDAO usuarios) {
    return email -> {
      var u =
          usuarios
              .findByEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
              .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
      return User.withUsername(u.getEmail())
          .password(u.getSenhaHash())
          .roles(u.getPerfil())
          .disabled(!u.isAtivo())
          .build();
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/", "/login", "/cadastro", "/css/**", "/error")
                    .permitAll()
                    .requestMatchers("/clientes", "/clientes/**")
                    .hasRole("GESTOR")
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/painel", true).permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
        .build();
  }
}
