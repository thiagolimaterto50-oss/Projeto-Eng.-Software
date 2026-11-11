package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.ResponsavelConfirmacao;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IResponsavelConfirmacaoDAO extends CrudRepository<ResponsavelConfirmacao, Long> {
  List<ResponsavelConfirmacao> findAll();
  java.util.Optional<ResponsavelConfirmacao> findFirstByOrderByIdAsc();
}
