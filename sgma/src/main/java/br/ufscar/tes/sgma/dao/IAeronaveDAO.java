package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.Aeronave;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface IAeronaveDAO extends CrudRepository<Aeronave, Long> {
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "cliente")
  List<Aeronave> findAll();

  boolean existsByMatriculaIgnoreCaseAndAtivaTrue(String matricula);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select a from Aeronave a where a.id = :id")
  java.util.Optional<Aeronave> buscarParaAlteracao(
      @org.springframework.data.repository.query.Param("id") Long id);
}
