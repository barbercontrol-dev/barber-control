package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.MovimentoFinanceiro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, Long> {

    @EntityGraph(attributePaths = "servicos")
    List<MovimentoFinanceiro> findByUsuarioIdAndTipoAndDataBetweenOrderByDataDescIdDesc(
            Long usuarioId,
            String tipo,
            LocalDate inicio,
            LocalDate fim
    );

    Optional<MovimentoFinanceiro> findByAgendamentoId(Long agendamentoId);

    List<MovimentoFinanceiro> findByAgendamento_Barbeiro_Id(Long barbeiroId);

    void deleteAllByUsuarioId(Long usuarioId);
}
