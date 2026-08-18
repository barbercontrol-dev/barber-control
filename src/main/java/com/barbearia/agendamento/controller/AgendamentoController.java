package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.service.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<?> criarAgendamento(@RequestBody Agendamento agendamento) {
    try {
        Agendamento criado = agendamentoService.criarAgendamento(agendamento);
        return ResponseEntity.ok(criado);
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
    }
}

    @GetMapping
    public List<Agendamento> listarAgendamentos() {
        return agendamentoService.listarTodos();
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public List<Agendamento> listarPorBarbeiro(@PathVariable Long barbeiroId) {
        return agendamentoService.listarPorBarbeiro(barbeiroId);
    }

    @GetMapping("/{id}")
    public Optional<Agendamento> buscarPorId(@PathVariable Long id) {
        return agendamentoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarAgendamento(@PathVariable Long id, @RequestBody Agendamento agendamentoAtualizado) {
        try {
            Agendamento atualizado = agendamentoService.atualizarAgendamento(id, agendamentoAtualizado);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public Agendamento atualizarStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return agendamentoService.atualizarStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    public void deletarAgendamento(@PathVariable Long id) {
        agendamentoService.deletarAgendamento(id);
    }
}