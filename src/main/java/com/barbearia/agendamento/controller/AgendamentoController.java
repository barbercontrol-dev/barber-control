package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.AgendamentoService;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<?> criarAgendamento(@RequestBody Agendamento agendamento,
                                              Authentication authentication) {
    try {
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        Long barbeiroId = agendamento.getBarbeiro() != null ? agendamento.getBarbeiro().getId() : null;
        if (barbeiroId == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Selecione um barbeiro."));
        }
        Usuario barbeiro = usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, responsavel.getId());
        agendamento.setBarbeiro(barbeiro);
        Agendamento criado = agendamentoService.criarAgendamento(agendamento);
        return ResponseEntity.ok(criado);
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
    }
}

    @GetMapping
    public ResponseEntity<?> listarAgendamentos(@RequestParam(required = false) Long barbeiroId,
                                                 Authentication authentication) {
        try {
            Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
            if (barbeiroId != null) {
                usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, responsavel.getId());
            }
            return ResponseEntity.ok(agendamentoService.listarDaBarbearia(responsavel.getId(), barbeiroId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public ResponseEntity<?> listarPorBarbeiro(@PathVariable Long barbeiroId,
                                               Authentication authentication) {
        try {
            Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
            usuarioService.buscarBarbeiroDaBarbearia(barbeiroId, responsavel.getId());
            return ResponseEntity.ok(agendamentoService.listarDaBarbearia(responsavel.getId(), barbeiroId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public Optional<Agendamento> buscarPorId(@PathVariable Long id, Authentication authentication) {
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        return agendamentoService.buscarPorIdDaBarbearia(id, responsavel.getId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarAgendamento(@PathVariable Long id,
                                                   @RequestBody Agendamento agendamentoAtualizado,
                                                   Authentication authentication) {
        try {
            Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
            if (agendamentoService.buscarPorIdDaBarbearia(id, responsavel.getId()).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Agendamento atualizado = agendamentoService.atualizarAgendamento(id, agendamentoAtualizado);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> body,
                                             Authentication authentication) {
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        if (agendamentoService.buscarPorIdDaBarbearia(id, responsavel.getId()).isEmpty()) {
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
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        if (agendamentoService.buscarPorIdDaBarbearia(id, responsavel.getId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        agendamentoService.deletarAgendamentoDoBarbeiro(
                id,
                agendamentoService.buscarPorIdDaBarbearia(id, responsavel.getId()).orElseThrow()
                        .getBarbeiro().getId()
        );
        return ResponseEntity.noContent().build();
    }
}