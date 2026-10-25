package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.MovimentoFinanceiroRequest;
import com.barbearia.agendamento.dto.MovimentoFinanceiroResponse;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.FinanceiroService;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final UsuarioService usuarioService;

    public FinanceiroController(FinanceiroService financeiroService, UsuarioService usuarioService) {
        this.financeiroService = financeiroService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> registrar(
            @RequestBody MovimentoFinanceiroRequest request,
            Authentication authentication
    ) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            MovimentoFinanceiroResponse response = financeiroService.registrar(request, usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/atendimentos-pendentes")
    public ResponseEntity<?> listarAtendimentosPendentes(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim,
            Authentication authentication
    ) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            return ResponseEntity.ok(
                    financeiroService.listarAtendimentosPendentes(usuario.getId(), inicio, fim)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumir(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim,
            Authentication authentication
    ) {
        try {
            Usuario usuario = usuarioService.buscarResponsavel(authentication.getName());
            return ResponseEntity.ok(financeiroService.resumir(usuario.getId(), inicio, fim));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam String tipo,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim,
            Authentication authentication
    ) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
            List<MovimentoFinanceiroResponse> movimentos =
                    financeiroService.listar(usuario.getId(), tipo, inicio, fim);
            return ResponseEntity.ok(movimentos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
