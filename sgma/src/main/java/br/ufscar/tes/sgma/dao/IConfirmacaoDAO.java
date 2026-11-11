package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Confirmacao;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IConfirmacaoDAO extends CrudRepository<Confirmacao, Long> {
  List<Confirmacao> findAll();
}
