package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    public Agendamento criarAgendamento(Agendamento agendamento) {
        Servico servicoCompleto = buscarServicoCompleto(agendamento.getServico().getId());
        agendamento.setServico(servicoCompleto);

        validarDisponibilidade(agendamento, null);
        agendamento.setStatus("AGENDADO");
        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> listarTodos() {
        return agendamentoRepository.findAll();
    }

    public List<Agendamento> listarPorBarbeiro(Long barbeiroId) {
        return agendamentoRepository.findByBarbeiroId(barbeiroId);
    }

    public Optional<Agendamento> buscarPorId(Long id) {
        return agendamentoRepository.findById(id);
    }

    public Agendamento atualizarAgendamento(Long id, Agendamento agendamentoAtualizado) {
        Optional<Agendamento> existente = agendamentoRepository.findById(id);
        if (existente.isPresent()) {
            Servico servicoCompleto = buscarServicoCompleto(agendamentoAtualizado.getServico().getId());
            agendamentoAtualizado.setServico(servicoCompleto);

            validarDisponibilidade(agendamentoAtualizado, id);

            Agendamento agendamento = existente.get();
            agendamento.setBarbeiro(agendamentoAtualizado.getBarbeiro());
            agendamento.setServico(servicoCompleto);
            agendamento.setClienteNome(agendamentoAtualizado.getClienteNome());
            agendamento.setClienteTelefone(agendamentoAtualizado.getClienteTelefone());
            agendamento.setDataHora(agendamentoAtualizado.getDataHora());
            return agendamentoRepository.save(agendamento);
        }
        return null;
    }

    public Agendamento atualizarStatus(Long id, String novoStatus) {
        Optional<Agendamento> existente = agendamentoRepository.findById(id);
        if (existente.isPresent()) {
            Agendamento agendamento = existente.get();
            agendamento.setStatus(novoStatus);
            return agendamentoRepository.save(agendamento);
        }
        return null;
    }

    public void deletarAgendamento(Long id) {
        agendamentoRepository.deleteById(id);
    }

    private Servico buscarServicoCompleto(Long servicoId) {
        return servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado com id: " + servicoId));
    }

    // Verifica se o novo horário sobrepõe algum agendamento já existente do mesmo barbeiro
    private void validarDisponibilidade(Agendamento novo, Long idParaIgnorar) {
        LocalDateTime novoInicio = novo.getDataHora();
        LocalDateTime novoFim = novoInicio.plusMinutes(novo.getServico().getDuracaoMinutos());

        List<Agendamento> agendamentosDoBarbeiro = agendamentoRepository.findByBarbeiroId(novo.getBarbeiro().getId());

        for (Agendamento existente : agendamentosDoBarbeiro) {
            if (existente.getId().equals(idParaIgnorar)) continue;
            if ("CANCELADO".equals(existente.getStatus())) continue;

            LocalDateTime inicioExistente = existente.getDataHora();
            LocalDateTime fimExistente = inicioExistente.plusMinutes(existente.getServico().getDuracaoMinutos());

            boolean sobrepoe = novoInicio.isBefore(fimExistente) && inicioExistente.isBefore(novoFim);
            if (sobrepoe) {
                throw new IllegalStateException("Já existe um agendamento para esse barbeiro nesse horário.");
            }
        }
    }
}