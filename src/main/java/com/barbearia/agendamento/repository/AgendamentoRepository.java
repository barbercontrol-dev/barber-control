package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByBarbeiroId(Long barbeiroId);

    Optional<Agendamento> findByIdAndBarbeiroId(Long id, Long barbeiroId);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.barbeiro.id = :barbeiroId
              AND a.status = 'CONCLUIDO'
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
              AND NOT EXISTS (
                  SELECT m.id FROM MovimentoFinanceiro m WHERE m.agendamento.id = a.id
              )
            ORDER BY a.dataHora DESC
            """)
    List<Agendamento> findConcluidosSemEntradaFinanceira(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}