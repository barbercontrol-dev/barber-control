package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BarbeiroContaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public BarbeiroContaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validar(String email, String senha, boolean senhaObrigatoria, Long idAtual) {
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        usuarioRepository.findByEmail(email.trim())
                .filter(u -> !u.getId().equals(idAtual))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Este e-mail já está em uso.");
                });
        boolean temSenha = senha != null && !senha.isBlank();
        if ((senhaObrigatoria && !temSenha) || (temSenha && senha.length() < 8)) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }
    }

    public Usuario definirCredenciais(Usuario barbeiro, String email, String senha) {
        barbeiro.setEmail(email.trim());
        if (senha != null && !senha.isBlank()) {
            barbeiro.setSenha(passwordEncoder.encode(senha));
        }
        return usuarioRepository.save(barbeiro);
    }
}