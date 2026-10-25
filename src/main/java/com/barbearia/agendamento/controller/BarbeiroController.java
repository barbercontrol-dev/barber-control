package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.BarbeiroRequest;
import com.barbearia.agendamento.dto.BarbeiroCadastroResponse;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/barbeiros")
public class BarbeiroController {

    private final UsuarioService usuarioService;

    public BarbeiroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<BarbeiroCadastroResponse> listar(Authentication authentication) {
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        return usuarioService.listarBarbeiros(responsavel.getId()).stream()
                .map(barbeiro -> new BarbeiroCadastroResponse(
                        barbeiro.getId(), barbeiro.getNome(), barbeiro.getTelefone(), barbeiro.getCpf()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody BarbeiroRequest request, Authentication authentication) {
        try {
            Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
            Usuario barbeiro = usuarioService.cadastrarBarbeiro(
                    request.nome(), request.telefone(), request.cpf(), responsavel);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new BarbeiroCadastroResponse(
                            barbeiro.getId(), barbeiro.getNome(), barbeiro.getTelefone(), barbeiro.getCpf()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestBody BarbeiroRequest request,
            Authentication authentication
    ) {
        try {
            Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
            Usuario barbeiro = usuarioService.atualizarBarbeiro(
                    id, request.nome(), request.telefone(), request.cpf(), responsavel.getId());
            return ResponseEntity.ok(new BarbeiroCadastroResponse(
                    barbeiro.getId(), barbeiro.getNome(), barbeiro.getTelefone(), barbeiro.getCpf()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        Usuario responsavel = usuarioService.buscarResponsavel(authentication.getName());
        return usuarioService.excluirBarbeiro(id, responsavel.getId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
