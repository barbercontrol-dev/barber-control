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
              AND (a.barbeiro.barbearia.id = :barbeariaId OR a.barbeiro.id = :barbeariaId)
            ORDER BY a.dataHora
            """)
    List<Agendamento> findByBarbeiroDaBarbearia(
            @Param("barbeiroId") Long barbeiroId,
            @Param("barbeariaId") Long barbeariaId
    );

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.barbeiro.barbearia.id = :barbeariaId OR a.barbeiro.id = :barbeariaId
            ORDER BY a.dataHora
            """)
    List<Agendamento> findByBarbearia(@Param("barbeariaId") Long barbeariaId);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.id = :id
              AND (a.barbeiro.barbearia.id = :barbeariaId OR a.barbeiro.id = :barbeariaId)
            """)
    Optional<Agendamento> findByIdAndBarbearia(
            @Param("id") Long id,
            @Param("barbeariaId") Long barbeariaId
    );

    @Query("""
            SELECT a FROM Agendamento a
            WHERE (a.barbeiro.barbearia.id = :barbeiroId OR a.barbeiro.id = :barbeiroId)
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

    @Query("""
            SELECT a FROM Agendamento a
            JOIN FETCH a.servico
            WHERE (a.barbeiro.barbearia.id = :barbeariaId OR a.barbeiro.id = :barbeariaId)
              AND a.status = 'CONCLUIDO'
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
            ORDER BY a.dataHora
            """)
    List<Agendamento> findConcluidosDaBarbearia(
            @Param("barbeariaId") Long barbeariaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}