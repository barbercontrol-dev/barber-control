package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByBarbeiroId(Long barbeiroId);
}