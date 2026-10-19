package com.barbearia.agendamento.service;

import com.barbearia.agendamento.dto.AtendimentoPendenteFinanceiroResponse;
import com.barbearia.agendamento.dto.MovimentoFinanceiroRequest;
import com.barbearia.agendamento.dto.MovimentoFinanceiroResponse;
import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.MovimentoFinanceiro;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.ServicoMovimentoFinanceiro;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.MovimentoFinanceiroRepository;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FinanceiroService {

    private static final Set<String> TIPOS = Set.of("ENTRADA", "SAIDA");
    private static final Set<String> FORMAS_PAGAMENTO = Set.of("DINHEIRO", "PIX", "DEBITO", "CREDITO");

    private final MovimentoFinanceiroRepository movimentoRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public FinanceiroService(
            MovimentoFinanceiroRepository movimentoRepository,
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository
    ) {
        this.movimentoRepository = movimentoRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AtendimentoPendenteFinanceiroResponse> listarAtendimentosPendentes(
            Long usuarioId,
            LocalDate inicio,
            LocalDate fim
    ) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Informe um período válido.");
        }
        return agendamentoRepository.findConcluidosSemEntradaFinanceira(
                        usuarioId,
                        inicio.atStartOfDay(),
                        fim.plusDays(1).atStartOfDay()
                )
                .stream()
                .map(agendamento -> new AtendimentoPendenteFinanceiroResponse(
                        agendamento.getId(),
                        agendamento.getDataHora().toLocalDate(),
                        agendamento.getClienteNome(),
                        agendamento.getServico().getNome(),
                        agendamento.getServico().getValor()
                ))
                .toList();
    }

    @Transactional
    public void registrarEntradaAgendamento(Agendamento agendamento, String formaPagamento) {
        validarFormaPagamento(formaPagamento);
        if (movimentoRepository.findByAgendamentoId(agendamento.getId()).isPresent()) {
            return;
        }

        Servico servico = agendamento.getServico();
        if (servico == null || servico.getId() == null || servico.getNome() == null || servico.getValor() == null) {
            throw new IllegalArgumentException("O serviço do agendamento não possui dados financeiros válidos.");
        }

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipo("ENTRADA");
        movimento.setData(agendamento.getDataHora().toLocalDate());
        movimento.setNomeCliente(agendamento.getClienteNome());
        movimento.setFormaPagamento(formaPagamento);
        movimento.setValor(servico.getValor());
        movimento.setUsuario(agendamento.getBarbeiro());
        movimento.setAgendamento(agendamento);

        ServicoMovimentoFinanceiro item = new ServicoMovimentoFinanceiro();
        item.setServicoId(servico.getId());
        item.setNome(servico.getNome());
        item.setValor(servico.getValor());
        movimento.adicionarServico(item);
        movimentoRepository.save(movimento);
    }

    @Transactional
    public void removerEntradaAgendamento(Long agendamentoId) {
        movimentoRepository.findByAgendamentoId(agendamentoId).ifPresent(movimentoRepository::delete);
    }

    @Transactional
    public MovimentoFinanceiroResponse registrar(MovimentoFinanceiroRequest request, Usuario usuario) {
        if (request == null) {
            throw new IllegalArgumentException("Informe os dados da operação.");
        }
        validarTipo(request.tipo());
        validarFormaPagamento(request.formaPagamento());

        if (request.data() == null) {
            throw new IllegalArgumentException("Informe a data da operação.");
        }

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipo(request.tipo());
        movimento.setData(request.data());
        movimento.setFormaPagamento(request.formaPagamento());
        movimento.setUsuario(usuario);

        if ("ENTRADA".equals(request.tipo())) {
            String nomeCliente = textoObrigatorio(request.nomeCliente(), "Informe o nome do cliente.");
            if (nomeCliente.length() > 120) {
                throw new IllegalArgumentException("O nome do cliente deve ter no máximo 120 caracteres.");
            }
            movimento.setNomeCliente(nomeCliente);
            movimento.setValor(registrarServicos(request.servicoIds(), movimento));
        } else {
            String descricao = textoObrigatorio(request.descricao(), "Informe a descrição da despesa.");
            String categoria = textoObrigatorio(request.categoria(), "Informe a categoria da despesa.");
            if (descricao.length() > 180 || categoria.length() > 80) {
                throw new IllegalArgumentException("Descrição ou categoria acima do limite permitido.");
            }
            if (request.valor() == null || request.valor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("O valor da despesa deve ser maior que zero.");
            }
            if (request.valor().compareTo(new BigDecimal("99999999.99")) > 0 || request.valor().scale() > 2) {
                throw new IllegalArgumentException("O valor da despesa deve ter no máximo duas casas decimais e não ultrapassar R$ 99.999.999,99.");
            }
            movimento.setDescricao(descricao);
            movimento.setCategoria(categoria);
            movimento.setValor(request.valor());
        }

        return toResponse(movimentoRepository.save(movimento));
    }

    @Transactional(readOnly = true)
    public List<MovimentoFinanceiroResponse> listar(
            Long usuarioId,
            String tipo,
            LocalDate inicio,
            LocalDate fim
    ) {
        validarTipo(tipo);
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Informe um período válido.");
        }

        return movimentoRepository
                .findByUsuarioIdAndTipoAndDataBetweenOrderByDataDescIdDesc(usuarioId, tipo, inicio, fim)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal registrarServicos(List<Long> servicoIds, MovimentoFinanceiro movimento) {
        if (servicoIds == null || servicoIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um serviço.");
        }
        if (servicoIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(servicoIds).size() != servicoIds.size()) {
            throw new IllegalArgumentException("A lista de serviços contém identificadores inválidos.");
        }

        Map<Long, Servico> servicosPorId = new HashMap<>();
        servicoRepository.findAllById(servicoIds).forEach(servico -> servicosPorId.put(servico.getId(), servico));
        if (servicosPorId.size() != servicoIds.size()) {
            throw new IllegalArgumentException("Um ou mais serviços selecionados não existem.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Long servicoId : servicoIds) {
            Servico servico = servicosPorId.get(servicoId);
            if (servico.getNome() == null || servico.getValor() == null) {
                throw new IllegalArgumentException("Um dos serviços selecionados não possui nome ou valor válidos.");
            }
            ServicoMovimentoFinanceiro item = new ServicoMovimentoFinanceiro();
            item.setServicoId(servico.getId());
            item.setNome(servico.getNome());
            item.setValor(servico.getValor());
            movimento.adicionarServico(item);
            total = total.add(servico.getValor());
        }
        return total;
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private void validarTipo(String tipo) {
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de operação inválido.");
        }
    }

    private void validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || !FORMAS_PAGAMENTO.contains(formaPagamento)) {
            throw new IllegalArgumentException("Forma de pagamento inválida.");
        }
    }

    private MovimentoFinanceiroResponse toResponse(MovimentoFinanceiro movimento) {
        List<MovimentoFinanceiroResponse.ServicoRegistrado> servicos = new ArrayList<>();
        movimento.getServicos().forEach(servico -> servicos.add(
                new MovimentoFinanceiroResponse.ServicoRegistrado(
                        servico.getServicoId(),
                        servico.getNome(),
                        servico.getValor()
                )
        ));

        return new MovimentoFinanceiroResponse(
                movimento.getId(),
                movimento.getTipo(),
                movimento.getData(),
                movimento.getNomeCliente(),
                movimento.getDescricao(),
                movimento.getCategoria(),
                movimento.getFormaPagamento(),
                movimento.getValor(),
                movimento.getUsuario().getNome(),
                servicos
        );
    }
}
