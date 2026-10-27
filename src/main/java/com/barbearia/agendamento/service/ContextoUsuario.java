package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ContextoUsuario {

    public record Atual(Usuario usuario, Long barbeariaId, boolean admin) {
    }

    private final UsuarioRepository usuarioRepository;

    public ContextoUsuario(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Atual de(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        boolean admin = "ADMIN".equals(usuario.getRole());
        Long barbeariaId = (!admin && usuario.getBarbearia() != null)
                ? usuario.getBarbearia().getId()
                : usuario.getId();
        return new Atual(usuario, barbeariaId, admin);
    }
}