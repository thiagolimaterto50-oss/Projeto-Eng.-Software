package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Usuario;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IUsuarioDAO extends CrudRepository<Usuario, Long> {
  List<Usuario> findAll();

  boolean existsByEmailIgnoreCase(String email);
}
