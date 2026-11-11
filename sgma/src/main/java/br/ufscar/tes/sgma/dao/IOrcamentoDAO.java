package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Orcamento;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IOrcamentoDAO extends CrudRepository<Orcamento, Long> {
  List<Orcamento> findAll();
}
