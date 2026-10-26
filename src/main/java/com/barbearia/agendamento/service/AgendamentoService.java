package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private FinanceiroService financeiroService;

    @Transactional
    public Agendamento criarAgendamento(Agendamento agendamento) {
        Long barbeariaId = agendamento.getBarbeiro().getBarbearia() != null
                ? agendamento.getBarbeiro().getBarbearia().getId()
                : agendamento.getBarbeiro().getId();
        List<Servico> servicosCompletos = resolverServicos(agendamento, barbeariaId);
        agendamento.setServicos(new ArrayList<>(servicosCompletos));
        agendamento.setServico(servicosCompletos.get(0));

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

    public List<Agendamento> listarDaBarbearia(Long barbeariaId, Long barbeiroId) {
        if (barbeiroId == null) {
            return agendamentoRepository.findByBarbearia(barbeariaId);
        }
        return agendamentoRepository.findByBarbeiroDaBarbearia(barbeiroId, barbeariaId);
    }

    public Optional<Agendamento> buscarPorId(Long id) {
        return agendamentoRepository.findById(id);
    }

    public Optional<Agendamento> buscarPorIdDoBarbeiro(Long id, Long barbeiroId) {
        return agendamentoRepository.findByIdAndBarbeiroId(id, barbeiroId);
    }

    public Optional<Agendamento> buscarPorIdDaBarbearia(Long id, Long barbeariaId) {
        return agendamentoRepository.findByIdAndBarbearia(id, barbeariaId);
    }

    @Transactional
    public Agendamento atualizarAgendamento(Long id, Agendamento agendamentoAtualizado) {
        Optional<Agendamento> existente = agendamentoRepository.findById(id);
        if (existente.isPresent()) {
            Agendamento agendamento = existente.get();
            agendamentoAtualizado.setBarbeiro(agendamento.getBarbeiro());
            Long barbeariaId = agendamento.getBarbeiro().getBarbearia() != null
                    ? agendamento.getBarbeiro().getBarbearia().getId()
                    : agendamento.getBarbeiro().getId();
            List<Servico> servicosCompletos = resolverServicos(agendamentoAtualizado, barbeariaId);
            agendamentoAtualizado.setServicos(new ArrayList<>(servicosCompletos));
            agendamentoAtualizado.setServico(servicosCompletos.get(0));

            validarDisponibilidade(agendamentoAtualizado, id);

            agendamento.setServicos(new ArrayList<>(servicosCompletos));
            agendamento.setServico(servicosCompletos.get(0));
            agendamento.setClienteNome(agendamentoAtualizado.getClienteNome());
            agendamento.setClienteTelefone(agendamentoAtualizado.getClienteTelefone());
            agendamento.setDataHora(agendamentoAtualizado.getDataHora());
            return agendamentoRepository.save(agendamento);
        }
        return null;
    }

    @Transactional
    public Agendamento atualizarStatus(Long id, String novoStatus, String formaPagamento) {
        if (novoStatus == null || !List.of("AGENDADO", "CONCLUIDO", "CANCELADO").contains(novoStatus)) {
            throw new IllegalArgumentException("Status de agendamento inválido.");
        }
        Optional<Agendamento> existente = agendamentoRepository.findById(id);
        if (existente.isPresent()) {
            Agendamento agendamento = existente.get();
            if ("CONCLUIDO".equals(novoStatus)) {
                financeiroService.registrarEntradaAgendamento(agendamento, formaPagamento);
            } else if ("CONCLUIDO".equals(agendamento.getStatus())) {
                financeiroService.removerEntradaAgendamento(id);
            }
            agendamento.setStatus(novoStatus);
            return agendamentoRepository.save(agendamento);
        }
        return null;
    }

    public void deletarAgendamento(Long id) {
        financeiroService.removerEntradaAgendamento(id);
        agendamentoRepository.deleteById(id);
    }

    public void deletarAgendamentoDoBarbeiro(Long id, Long barbeiroId) {
        agendamentoRepository.findByIdAndBarbeiroId(id, barbeiroId).ifPresent(agendamento -> {
            financeiroService.removerEntradaAgendamento(id);
            agendamentoRepository.delete(agendamento);
        });
    }

    private Servico buscarServicoCompleto(Long servicoId, Long barbeariaId) {
        return servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado nesta barbearia."));
    }

    private List<Servico> resolverServicos(Agendamento agendamento, Long barbeariaId) {
        List<Servico> solicitados = agendamento.getServicos();
        if (solicitados == null || solicitados.isEmpty()) {
            if (agendamento.getServico() == null) {
                throw new IllegalArgumentException("Selecione ao menos um serviço.");
            }
            solicitados = List.of(agendamento.getServico());
        }

        List<Long> ids = solicitados.stream()
                .map(servico -> servico == null ? null : servico.getId())
                .toList();
        if (ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("A lista de serviços contém identificadores inválidos.");
        }

        return ids.stream()
                .map(id -> buscarServicoCompleto(id, barbeariaId))
                .toList();
    }

    private int duracaoTotal(Agendamento agendamento) {
        List<Servico> servicos = agendamento.getServicos();
        if (servicos == null || servicos.isEmpty()) {
            servicos = agendamento.getServico() == null
                    ? List.of()
                    : List.of(agendamento.getServico());
        }
        return servicos.stream()
                .filter(servico -> servico != null && servico.getDuracaoMinutos() != null)
                .mapToInt(Servico::getDuracaoMinutos)
                .sum();
    }

    // Verifica se o novo horário sobrepõe algum agendamento já existente do mesmo barbeiro
    private void validarDisponibilidade(Agendamento novo, Long idParaIgnorar) {
        if (novo.getDataHora().toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Não é possível agendar em um dia que já passou.");
        }

        LocalDateTime novoInicio = novo.getDataHora();
        LocalDateTime novoFim = novoInicio.plusMinutes(duracaoTotal(novo));

        List<Agendamento> agendamentosDoBarbeiro = agendamentoRepository.findByBarbeiroId(novo.getBarbeiro().getId());

        for (Agendamento existente : agendamentosDoBarbeiro) {
            if (existente.getId().equals(idParaIgnorar)) continue;
            if ("CANCELADO".equals(existente.getStatus())) continue;

            LocalDateTime inicioExistente = existente.getDataHora();
            LocalDateTime fimExistente = inicioExistente.plusMinutes(duracaoTotal(existente));

            boolean sobrepoe = novoInicio.isBefore(fimExistente) && inicioExistente.isBefore(novoFim);
            if (sobrepoe) {
                throw new IllegalStateException("Já existe um agendamento para esse barbeiro nesse horário.");
            }
        }
    }
}