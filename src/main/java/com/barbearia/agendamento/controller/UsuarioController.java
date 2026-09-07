package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/meus-dados")
    public Usuario meusDados() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @DeleteMapping("/meus-dados")
    public String excluirConta() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        usuarioService.excluirConta(email);
        return "Conta e dados excluídos com sucesso.";
    }
}