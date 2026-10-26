package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.MeusDadosResponse;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import com.barbearia.agendamento.service.ContextoUsuario;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContextoUsuario contexto;

    @GetMapping
    public List<Usuario> listarUsuarios(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .map(List::of)
                .orElse(List.of());
    }

    @GetMapping("/meus-dados")
    public MeusDadosResponse meusDados(Authentication authentication) {
        var ctx = contexto.de(authentication);
        Usuario usuario = usuarioRepository.findByIdComBarbearia(ctx.usuario().getId()).orElseThrow();
        Usuario dono = ctx.admin() ? usuario : usuario.getBarbearia();
        return new MeusDadosResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                dono != null ? dono.getNomeBarbearia() : usuario.getNomeBarbearia(),
                dono != null ? dono.getEndereco() : null,
                ctx.admin() ? null : usuario.getId()
        );
    }

    @DeleteMapping("/meus-dados")
    public String excluirConta(Authentication authentication) {
        usuarioService.excluirConta(authentication.getName());
        return "Conta e dados excluídos com sucesso.";
    }
}