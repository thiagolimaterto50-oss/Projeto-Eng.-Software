package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.GestorSolicitacoes;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IGestorSolicitacoesDAO extends CrudRepository<GestorSolicitacoes, Long> {
  List<GestorSolicitacoes> findAll();

  java.util.Optional<GestorSolicitacoes> findFirstByOrderByIdAsc();
}
