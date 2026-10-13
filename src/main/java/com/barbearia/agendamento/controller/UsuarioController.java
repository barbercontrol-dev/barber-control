package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Devolve só o usuário logado
    // um barbeiro não enxerga os dados dos outros.
    @GetMapping
    public List<Usuario> listarUsuarios(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .map(List::of)
                .orElse(List.of());
    }

    @GetMapping("/meus-dados")
    public Usuario meusDados(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @DeleteMapping("/meus-dados")
    public String excluirConta(Authentication authentication) {
        usuarioService.excluirConta(authentication.getName());
        return "Conta e dados excluídos com sucesso.";
    }
}