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

import java.util.List;
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
        Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
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
    public List<Agendamento> listarAgendamentos(Authentication authentication) {
        Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
        return agendamentoService.listarPorBarbeiro(barbeiro.getId());
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public List<Agendamento> listarPorBarbeiro(@PathVariable Long barbeiroId,
                                               Authentication authentication) {
        Usuario barbeiroAutenticado = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
        return agendamentoService.listarPorBarbeiro(barbeiroAutenticado.getId());
    }

    @GetMapping("/{id}")
    public Optional<Agendamento> buscarPorId(@PathVariable Long id, Authentication authentication) {
        Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
        return agendamentoService.buscarPorIdDoBarbeiro(id, barbeiro.getId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarAgendamento(@PathVariable Long id,
                                                   @RequestBody Agendamento agendamentoAtualizado,
                                                   Authentication authentication) {
        try {
            Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
            if (agendamentoService.buscarPorIdDoBarbeiro(id, barbeiro.getId()).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Agendamento atualizado = agendamentoService.atualizarAgendamento(id, agendamentoAtualizado);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agendamento> atualizarStatus(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication authentication) {
        Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
        if (agendamentoService.buscarPorIdDoBarbeiro(id, barbeiro.getId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(agendamentoService.atualizarStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAgendamento(@PathVariable Long id, Authentication authentication) {
        Usuario barbeiro = usuarioService.buscarPorEmail(authentication.getName()).orElseThrow();
        if (agendamentoService.buscarPorIdDoBarbeiro(id, barbeiro.getId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        agendamentoService.deletarAgendamentoDoBarbeiro(id, barbeiro.getId());
        return ResponseEntity.noContent().build();
    }
}