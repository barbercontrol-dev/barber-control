package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.BarbeiroCadastroResponse;
import com.barbearia.agendamento.dto.BarbeiroRequest;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.BarbeiroContaService;
import com.barbearia.agendamento.service.ContextoUsuario;
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
    private final BarbeiroContaService contaService;
    private final ContextoUsuario contexto;

    public BarbeiroController(UsuarioService usuarioService,
                              BarbeiroContaService contaService,
                              ContextoUsuario contexto) {
        this.usuarioService = usuarioService;
        this.contaService = contaService;
        this.contexto = contexto;
    }

    private BarbeiroCadastroResponse resposta(Usuario b) {
        return new BarbeiroCadastroResponse(b.getId(), b.getNome(), b.getEmail(), b.getTelefone(), b.getCpf());
    }

    @GetMapping
    public List<BarbeiroCadastroResponse> listar(Authentication authentication) {
        var ctx = contexto.de(authentication);
        if (!ctx.admin()) {
            return List.of(resposta(ctx.usuario()));
        }
        return usuarioService.listarBarbeiros(ctx.barbeariaId()).stream().map(this::resposta).toList();
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody BarbeiroRequest request, Authentication authentication) {
        try {
            var ctx = contexto.de(authentication);
            contaService.validar(request.email(), request.senha(), true, null);
            Usuario barbeiro = usuarioService.cadastrarBarbeiro(
                    request.nome(), request.telefone(), request.cpf(), ctx.usuario());
            barbeiro = contaService.definirCredenciais(barbeiro, request.email(), request.senha());
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta(barbeiro));
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
            var ctx = contexto.de(authentication);
            contaService.validar(request.email(), request.senha(), false, id);
            Usuario barbeiro = usuarioService.atualizarBarbeiro(
                    id, request.nome(), request.telefone(), request.cpf(), ctx.barbeariaId());
            barbeiro = contaService.definirCredenciais(barbeiro, request.email(), request.senha());
            return ResponseEntity.ok(resposta(barbeiro));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        var ctx = contexto.de(authentication);
        return usuarioService.excluirBarbeiro(id, ctx.barbeariaId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}