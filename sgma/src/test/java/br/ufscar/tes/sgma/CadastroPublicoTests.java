package br.ufscar.tes.sgma;

import br.ufscar.tes.sgma.dao.*;
import br.ufscar.tes.sgma.domain.Cliente;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CadastroPublicoTests {
  @Autowired MockMvc mvc;
  @Autowired IUsuarioDAO usuarios;
  @Autowired PasswordEncoder encoder;

  private MockHttpServletRequestBuilder cadastro(String email, String documento) {
    return post("/cadastro").param("nome", "Cliente público").param("email", email)
        .param("documento", documento).param("telefone", "")
        .param("senha", "Cadastro123!").param("confirmacaoSenha", "Cadastro123!");
  }

  @Test void paginasPublicasEPainelProtegido() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("inicio"))
        .andExpect(content().string(containsString("Como funciona")));
    mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Cadastrar-se")));
    mvc.perform(get("/cadastro")).andExpect(status().isOk());
    mvc.perform(get("/painel")).andExpect(redirectedUrl("/login"));
    mvc.perform(get("/solicitacoes")).andExpect(redirectedUrl("/login"));
  }

  @Test void cadastroCriaApenasClienteEPermiteLogin() throws Exception {
    String chave=UUID.randomUUID().toString();
    String email=chave+"@publico.test";
    mvc.perform(cadastro(email, chave.substring(0,20)).with(csrf())
        .param("perfil","GESTOR").param("ativo","false").param("id","1"))
        .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("sucesso"));
    var u=usuarios.findByEmailIgnoreCase(email).orElseThrow();
    assertInstanceOf(Cliente.class,u);assertEquals("CLIENTE",u.getPerfil());assertTrue(u.isAtivo());
    assertTrue(encoder.matches("Cadastro123!",u.getSenhaHash()));
    var session=(org.springframework.mock.web.MockHttpSession)mvc.perform(post("/login").with(csrf())
        .param("username",email).param("password","Cadastro123!"))
        .andExpect(redirectedUrl("/painel")).andReturn().getRequest().getSession(false);
    mvc.perform(get("/painel").session(session)).andExpect(status().isOk()).andExpect(model().attribute("total",0L));
    mvc.perform(get("/clientes").session(session)).andExpect(status().isForbidden());
  }

  @Test void cadastroValidaConfirmacaoDadosDuplicidadeECsrf() throws Exception {
    String chave=UUID.randomUUID().toString().substring(0,12);String email=chave+"@publico.test";
    mvc.perform(cadastro(email,chave)).andExpect(status().isForbidden());
    mvc.perform(post("/cadastro").with(csrf()).param("nome","Cliente").param("email",email)
        .param("documento",chave).param("senha","Cadastro123!").param("confirmacaoSenha","Diferente123!"))
        .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"))
        .andExpect(content().string(containsString("As senhas não coincidem.")))
        .andExpect(content().string(not(containsString("value=\"Cadastro123!\""))));
    assertTrue(usuarios.findByEmailIgnoreCase(email).isEmpty());
    mvc.perform(post("/cadastro").with(csrf()).param("nome"," ").param("email","invalido")
        .param("documento","").param("senha","123").param("confirmacaoSenha","123"))
        .andExpect(model().attributeHasErrors("form"));
    mvc.perform(cadastro(email,chave).with(csrf())).andExpect(redirectedUrl("/login"));
    mvc.perform(cadastro(email.toUpperCase(),chave+"B").with(csrf()))
        .andExpect(model().attributeHasErrors("form")).andExpect(content().string(containsString("E-mail já cadastrado.")));
    mvc.perform(cadastro("outro-"+email,chave).with(csrf()))
        .andExpect(model().attributeHasErrors("form")).andExpect(content().string(containsString("Documento já cadastrado.")));
  }
}
