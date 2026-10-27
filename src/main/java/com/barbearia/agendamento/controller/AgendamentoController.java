package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.BloqueioRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import com.barbearia.agendamento.service.AgendamentoService;
import com.barbearia.agendamento.service.ContextoUsuario;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ContextoUsuario contexto;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private BloqueioRepository bloqueioRepository;

    private Optional<Agendamento> acessivel(Long id, ContextoUsuario.Atual ctx) {
        return agendamentoService.buscarPorIdDaBarbearia(id, ctx.barbeariaId())
                .filter(a -> ctx.admin() || a.getBarbeiro().getId().equals(ctx.usuario().getId()));
    }

    private ResponseEntity<?> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }

    private ResponseEntity<?> validarHorario(Usuario barbeiro, Long servicoId, LocalDateTime inicio, Long barbeariaId) {
        if (inicio == null || servicoId == null) {
            return erro(HttpStatus.BAD_REQUEST, "Informe o serviço e o horário.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            return erro(HttpStatus.BAD_REQUEST, "Esse horário já passou.");
        }
        Servico servico = servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId).orElse(null);
        if (servico == null || servico.getDuracaoMinutos() == null) {
            return erro(HttpStatus.BAD_REQUEST, "Serviço inválido.");
        }
        if (bloqueioRepository.existeConflito(barbeiro.getId(), inicio, inicio.plusMinutes(servico.getDuracaoMinutos()))) {
            return erro(HttpStatus.CONFLICT, "Horário indisponível para este barbeiro.");
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> criarAgendamento(@RequestBody Agendamento agendamento,
                                              Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            Usuario barbeiro;
            if (ctx.admin()) {
                Long barbeiroId = agendamento.getBarbeiro() != null ? agendamento.getBarbeiro().getId() : null;
                if (barbeiroId == null) {
                    return ResponseEntity.badRequest().body(Map.of("erro", "Selecione um barbeiro."));
                }
                barbeiro = usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, ctx.barbeariaId());
            } else {
                barbeiro = ctx.usuario();
            }

            Long servicoId = agendamento.getServico() != null ? agendamento.getServico().getId() : null;
            ResponseEntity<?> invalido = validarHorario(barbeiro, servicoId, agendamento.getDataHora(), ctx.barbeariaId());
            if (invalido != null) return invalido;

            agendamento.setBarbeiro(barbeiro);
            return ResponseEntity.ok(agendamentoService.criarAgendamento(agendamento));
        } catch (IllegalStateException e) {
            return erro(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return erro(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listarAgendamentos(@RequestParam(required = false) Long barbeiroId,
                                                Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            Long filtro = ctx.admin() ? barbeiroId : ctx.usuario().getId();
            if (ctx.admin() && barbeiroId != null) {
                usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, ctx.barbeariaId());
            }
            return ResponseEntity.ok(agendamentoService.listarDaBarbearia(ctx.barbeariaId(), filtro));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public ResponseEntity<?> listarPorBarbeiro(@PathVariable Long barbeiroId,
                                               Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            if (!ctx.admin() && !barbeiroId.equals(ctx.usuario().getId())) {
                return erro(HttpStatus.FORBIDDEN, "Você só pode ver a sua própria agenda.");
            }
            if (ctx.admin()) {
                usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, ctx.barbeariaId());
            }
            return ResponseEntity.ok(agendamentoService.listarDaBarbearia(ctx.barbeariaId(), barbeiroId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return acessivel(id, contexto.de(authentication))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarAgendamento(@PathVariable Long id,
                                                  @RequestBody Agendamento agendamentoAtualizado,
                                                  Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            Optional<Agendamento> existente = acessivel(id, ctx);
            if (existente.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Long servicoId = agendamentoAtualizado.getServico() != null
                    ? agendamentoAtualizado.getServico().getId()
                    : existente.get().getServico().getId();
            LocalDateTime inicio = agendamentoAtualizado.getDataHora() != null
                    ? agendamentoAtualizado.getDataHora()
                    : existente.get().getDataHora();
            ResponseEntity<?> invalido = validarHorario(existente.get().getBarbeiro(), servicoId, inicio, ctx.barbeariaId());
            if (invalido != null) return invalido;

            return ResponseEntity.ok(agendamentoService.atualizarAgendamento(id, agendamentoAtualizado));
        } catch (IllegalStateException e) {
            return erro(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return erro(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> body,
                                             Authentication authentication) {
        if (acessivel(id, contexto.de(authentication)).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(agendamentoService.atualizarStatus(
                    id,
                    body.get("status"),
                    body.get("formaPagamento")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAgendamento(@PathVariable Long id, Authentication authentication) {
        Optional<Agendamento> agendamento = acessivel(id, contexto.de(authentication));
        if (agendamento.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        agendamentoService.deletarAgendamentoDoBarbeiro(id, agendamento.get().getBarbeiro().getId());
        return ResponseEntity.noContent().build();
    }
}