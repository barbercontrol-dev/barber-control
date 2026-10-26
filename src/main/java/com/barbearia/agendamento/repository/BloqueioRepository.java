package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    List<Bloqueio> findByBarbeiroIdAndFimAfterOrderByInicio(Long barbeiroId, LocalDateTime agora);

    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueio b
            WHERE b.barbeiro.id = :barbeiroId
              AND b.inicio < :fim
              AND b.fim > :inicio
            """)
    boolean existeConflito(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}