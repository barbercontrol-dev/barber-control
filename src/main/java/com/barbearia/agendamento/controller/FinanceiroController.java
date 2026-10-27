package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.AtendimentoPendenteFinanceiroResponse;
import com.barbearia.agendamento.dto.MovimentoFinanceiroRequest;
import com.barbearia.agendamento.dto.MovimentoFinanceiroResponse;
import com.barbearia.agendamento.dto.ResumoFinanceiroResponse;
import com.barbearia.agendamento.model.MovimentoFinanceiro;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.MovimentoFinanceiroRepository;
import com.barbearia.agendamento.service.ContextoUsuario;
import com.barbearia.agendamento.service.FinanceiroService;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final UsuarioService usuarioService;
    private final ContextoUsuario contexto;
    private final MovimentoFinanceiroRepository movimentoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public FinanceiroController(FinanceiroService financeiroService,
                                UsuarioService usuarioService,
                                ContextoUsuario contexto,
                                MovimentoFinanceiroRepository movimentoRepository,
                                AgendamentoRepository agendamentoRepository) {
        this.financeiroService = financeiroService;
        this.usuarioService = usuarioService;
        this.contexto = contexto;
        this.movimentoRepository = movimentoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    private List<MovimentoFinanceiroResponse> entradasDoBarbeiro(Usuario barbeiro, LocalDate inicio, LocalDate fim) {
        return movimentoRepository.findByAgendamento_Barbeiro_Id(barbeiro.getId()).stream()
                .filter(m -> "ENTRADA".equalsIgnoreCase(m.getTipo()))
                .filter(m -> !m.getData().isBefore(inicio) && !m.getData().isAfter(fim))
                .sorted(Comparator.comparing(MovimentoFinanceiro::getData)
                        .thenComparing(MovimentoFinanceiro::getId).reversed())
                .map(m -> new MovimentoFinanceiroResponse(
                        m.getId(), m.getTipo(), m.getData(), m.getNomeCliente(), m.getDescricao(),
                        m.getCategoria(), m.getFormaPagamento(), m.getValor(), barbeiro.getNome(),
                        m.getAgendamento().getId(),
                        m.getServicos().stream()
                                .map(s -> new MovimentoFinanceiroResponse.ServicoRegistrado(
                                        s.getServicoId(), s.getNome(), s.getValor()))
                                .toList()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody MovimentoFinanceiroRequest request,
                                       Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            if (!ctx.admin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro",
                        "Só o administrador registra movimentos manuais. Seus atendimentos concluídos entram automaticamente."));
            }
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            MovimentoFinanceiroResponse response = financeiroService.registrar(request, usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/atendimentos-pendentes")
    public ResponseEntity<?> listarAtendimentosPendentes(@RequestParam LocalDate inicio,
                                                         @RequestParam LocalDate fim,
                                                         Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            if (!ctx.admin()) {
                List<AtendimentoPendenteFinanceiroResponse> pendentes = agendamentoRepository
                        .findConcluidosSemEntradaFinanceira(
                                ctx.usuario().getId(), inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay())
                        .stream()
                        .map(a -> new AtendimentoPendenteFinanceiroResponse(
                                a.getId(), a.getDataHora().toLocalDate(), a.getClienteNome(),
                                a.getServico().getNome(), a.getServico().getValor()))
                        .toList();
                return ResponseEntity.ok(pendentes);
            }
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            return ResponseEntity.ok(financeiroService.listarAtendimentosPendentes(usuario.getId(), inicio, fim));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/resumo")
    public ResponseEntity<?> resumir(@RequestParam LocalDate inicio,
                                     @RequestParam LocalDate fim,
                                     Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            if (!ctx.admin()) {
                List<MovimentoFinanceiroResponse> entradas = entradasDoBarbeiro(ctx.usuario(), inicio, fim);
                BigDecimal faturamento = entradas.stream()
                        .map(MovimentoFinanceiroResponse::valor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return ResponseEntity.ok(new ResumoFinanceiroResponse(
                        faturamento, BigDecimal.ZERO, faturamento, entradas.size()));
            }
            Usuario usuario = usuarioService.buscarResponsavel(authentication.getName());
            return ResponseEntity.ok(financeiroService.resumir(usuario.getId(), inicio, fim));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam String tipo,
                                    @RequestParam LocalDate inicio,
                                    @RequestParam LocalDate fim,
                                    Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            if (!ctx.admin()) {
                return ResponseEntity.ok("ENTRADA".equalsIgnoreCase(tipo)
                        ? entradasDoBarbeiro(ctx.usuario(), inicio, fim)
                        : List.<MovimentoFinanceiroResponse>of());
            }
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            return ResponseEntity.ok(financeiroService.listar(usuario.getId(), tipo, inicio, fim));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}