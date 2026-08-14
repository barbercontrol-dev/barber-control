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
    public Servico criarServico(Servico servico) {
        return servicoRepository.save(servico);
    }


    public List<Servico> listarTodos() {
        return servicoRepository.findAll();
    }


    public Optional<Servico> buscarPorId(Long id) {
        return servicoRepository.findById(id);
    }


    public Servico atualizarServico(Long id, Servico servicoAtualizado) {
        Optional<Servico> servicoExistente = servicoRepository.findById(id);
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


    public void deletarServico(Long id) {
        servicoRepository.deleteById(id);
    }
}
