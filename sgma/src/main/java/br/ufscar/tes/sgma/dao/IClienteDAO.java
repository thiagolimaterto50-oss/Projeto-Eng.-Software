package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Cliente;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IClienteDAO extends CrudRepository<Cliente, Long> {
  List<Cliente> findAll();

  boolean existsByDocumento(String documento);
}
