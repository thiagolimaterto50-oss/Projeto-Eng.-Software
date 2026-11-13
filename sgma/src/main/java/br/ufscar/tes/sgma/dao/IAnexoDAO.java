package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Anexo;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface IAnexoDAO extends CrudRepository<Anexo, Long> {
  Optional<Anexo> findBySolicitacaoId(Long id);
}
