package br.ufscar.tes.sgma.dao;

import br.ufscar.tes.sgma.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ISolicitacaoDAO extends CrudRepository<Solicitacao, Long> {
  long countBySituacao(SituacaoSolicitacao situacao);

  @Query(
      """
        select s from Solicitacao s
        where (:clienteId is null or s.cliente.id = :clienteId)
          and (:situacao is null or s.situacao = :situacao)
          and (lower(s.descricao) like :busca escape '!'
            or lower(s.aeronave.matricula) like :busca escape '!'
            or lower(s.cliente.nome) like :busca escape '!')
        """)
  Page<Solicitacao> pesquisar(
      @Param("situacao") SituacaoSolicitacao situacao,
      @Param("busca") String busca,
      @Param("clienteId") Long clienteId,
      Pageable pageable);

  @Query(
      "select count(s) from Solicitacao s where (:clienteId is null or s.cliente.id = :clienteId)"
          + " and (:situacao is null or s.situacao = :situacao)")
  long contar(@Param("clienteId") Long clienteId, @Param("situacao") SituacaoSolicitacao situacao);
}
