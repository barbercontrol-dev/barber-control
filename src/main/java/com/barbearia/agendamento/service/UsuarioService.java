package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public Usuario cadastrar(Usuario usuario) {
        if (usuario.getConsentimentoLgpd() == null || !usuario.getConsentimentoLgpd()) {
            throw new IllegalArgumentException("É necessário aceitar os termos de uso e a política de privacidade para se cadastrar.");
        }

        usuario.setSenha(encoder.encode(usuario.getSenha()));
        usuario.setRole("ADMIN");
        usuario.setDataConsentimento(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean validarSenha(String senhaDigitada, String senhaArmazenada) {
        return encoder.matches(senhaDigitada, senhaArmazenada);
    }

    // Direito de exclusão: apaga os agendamentos do barbeiro
    // e depois a conta. Tudo na mesma transação: ou apaga tudo, ou nada.
    @Transactional
    public void excluirConta(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        agendamentoRepository.deleteAll(agendamentoRepository.findByBarbeiroId(usuario.getId()));
        usuarioRepository.delete(usuario);
    }
}