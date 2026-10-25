package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class ServicoService {
    @Autowired
    private ServicoRepository servicoRepository;

    // Criar nov o serviço
    public Servico criarServico(Servico servico, Long barbeariaId) {
        servico.setBarbeariaId(barbeariaId);
        return servicoRepository.save(servico);
    }


    public List<Servico> listarTodos(Long barbeariaId) {
        return servicoRepository.findByBarbeariaIdOrderById(barbeariaId);
    }


    public Optional<Servico> buscarPorId(Long id, Long barbeariaId) {
        return servicoRepository.findByIdAndBarbeariaId(id, barbeariaId);
    }


    public Servico atualizarServico(Long id, Servico servicoAtualizado, Long barbeariaId) {
        Optional<Servico> servicoExistente = servicoRepository.findByIdAndBarbeariaId(id, barbeariaId);
        if (servicoExistente.isPresent()) {
            Servico servico = servicoExistente.get();
            servico.setNome(servicoAtualizado.getNome());
            servico.setValor(servicoAtualizado.getValor());
            servico.setDuracaoMinutos(servicoAtualizado.getDuracaoMinutos());
            return servicoRepository.save(servico);
        } else {
            return null;
        }
    }


    public boolean deletarServico(Long id, Long barbeariaId) {
        return servicoRepository.findByIdAndBarbeariaId(id, barbeariaId)
                .map(servico -> {
                    servicoRepository.delete(servico);
                    return true;
                })
                .orElse(false);
    }
}
