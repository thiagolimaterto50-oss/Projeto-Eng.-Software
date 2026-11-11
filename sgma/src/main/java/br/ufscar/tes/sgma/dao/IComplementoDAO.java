package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Complemento;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IComplementoDAO extends CrudRepository<Complemento, Long> {
  List<Complemento> findAll();
}
