package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByBarbeariaIdOrderById(Long barbeariaId);

    Optional<Servico> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    void deleteAllByBarbeariaId(Long barbeariaId);
}
