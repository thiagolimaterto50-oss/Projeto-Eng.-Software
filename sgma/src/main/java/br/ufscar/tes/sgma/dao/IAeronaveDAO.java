package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Aeronave;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IAeronaveDAO extends CrudRepository<Aeronave, Long> {
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "cliente")
  List<Aeronave> findAll();

  boolean existsByMatriculaIgnoreCaseAndAtivaTrue(String matricula);
}
