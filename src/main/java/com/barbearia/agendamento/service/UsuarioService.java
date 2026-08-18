package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Codificador de senhas (BCrypt) — usado para criptografar antes de salvar
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Cadastra um novo usuário, criptografa a senha e define a role como ADMIN
    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public Usuario cadastrar(Usuario usuario) {
        usuario.setSenha(encoder.encode(usuario.getSenha()));
        usuario.setRole("ADMIN");
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // Compara a senha digitada com a senha armazenada (já criptografada)

    public boolean validarSenha(String senhaDigitada, String senhaArmazenada) {
        return encoder.matches(senhaDigitada, senhaArmazenada);
    }
}
