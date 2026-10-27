package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.BloqueioRequest;
import com.barbearia.agendamento.dto.BloqueioResponse;
import com.barbearia.agendamento.model.Bloqueio;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.BloqueioRepository;
import com.barbearia.agendamento.service.ContextoUsuario;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/bloqueios")
public class BloqueioController {

    private final BloqueioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioService usuarioService;
    private final ContextoUsuario contexto;

    public BloqueioController(BloqueioRepository bloqueioRepository,
                              AgendamentoRepository agendamentoRepository,
                              UsuarioService usuarioService,
                              ContextoUsuario contexto) {
        this.bloqueioRepository = bloqueioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioService = usuarioService;
        this.contexto = contexto;
    }

    private BloqueioResponse resposta(Bloqueio b) {
        return new BloqueioResponse(b.getId(), b.getBarbeiro().getId(), b.getInicio(), b.getFim());
    }

    private Usuario barbeiroAlvo(ContextoUsuario.Atual ctx, Long barbeiroId) {
        if (!ctx.admin()) return ctx.usuario();
        if (barbeiroId == null) throw new IllegalArgumentException("Selecione um barbeiro.");
        return usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, ctx.barbeariaId());
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long barbeiroId, Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            Usuario barbeiro = barbeiroAlvo(ctx, barbeiroId);
            return ResponseEntity.ok(bloqueioRepository
                    .findByBarbeiroIdAndFimAfterOrderByInicio(barbeiro.getId(), LocalDateTime.now())
                    .stream().map(this::resposta).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody BloqueioRequest request, Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            Usuario barbeiro = barbeiroAlvo(ctx, request.barbeiro() != null ? request.barbeiro().id() : null);

            LocalDateTime inicio = request.inicio();
            LocalDateTime fim = request.fim();
            if (inicio == null || fim == null || !fim.isAfter(inicio)) {
                return ResponseEntity.badRequest().body(Map.of("erro", "O horário final deve ser depois do inicial."));
            }
            if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
                return ResponseEntity.badRequest().body(Map.of("erro", "O bloqueio deve estar dentro de um único dia."));
            }
            if (fim.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Esse horário já passou."));
            }

            boolean temAgendamento = agendamentoRepository.findByBarbeiroId(barbeiro.getId()).stream()
                    .filter(a -> !"CANCELADO".equals(a.getStatus()))
                    .anyMatch(a -> {
                        Integer duracao = a.getServico() != null ? a.getServico().getDuracaoMinutos() : null;
                        LocalDateTime fimAgendamento = a.getDataHora().plusMinutes(duracao == null ? 0 : duracao);
                        return inicio.isBefore(fimAgendamento) && a.getDataHora().isBefore(fim);
                    });
            if (temAgendamento) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("erro", "Já existem agendamentos nesse intervalo. Cancele-os antes de bloquear."));
            }

            Bloqueio bloqueio = new Bloqueio();
            bloqueio.setBarbeiro(barbeiro);
            bloqueio.setInicio(inicio);
            bloqueio.setFim(fim);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta(bloqueioRepository.save(bloqueio)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        var ctx = contexto.de(authentication);
        return bloqueioRepository.findById(id)
                .filter(b -> {
                    Usuario dono = b.getBarbeiro();
                    if (!ctx.admin()) return dono.getId().equals(ctx.usuario().getId());
                    return dono.getId().equals(ctx.barbeariaId())
                            || (dono.getBarbearia() != null && dono.getBarbearia().getId().equals(ctx.barbeariaId()));
                })
                .map(b -> {
                    bloqueioRepository.delete(b);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}