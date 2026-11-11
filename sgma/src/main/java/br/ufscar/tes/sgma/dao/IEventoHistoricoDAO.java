package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.EventoHistorico;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IEventoHistoricoDAO extends CrudRepository<EventoHistorico, Long> {
  List<EventoHistorico> findAll();
}
