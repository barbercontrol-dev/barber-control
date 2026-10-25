package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.dto.RegistroBarbeariaRequest;
import com.barbearia.agendamento.security.JwtUtil;
import com.barbearia.agendamento.service.UsuarioService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody RegistroBarbeariaRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrarBarbearia(
                    request.nomeBarbearia(),
                    request.nomeResponsavel(),
                    request.email(),
                    request.senha(),
                    request.consentimentoLgpd()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {
        var usuarioOpt = usuarioService.buscarPorEmail(usuario.getEmail());

        if (usuarioOpt.isPresent()
                && "ADMIN".equals(usuarioOpt.get().getRole())
                && usuarioService.validarSenha(usuario.getSenha(), usuarioOpt.get().getSenha())) {
            String token = jwtUtil.generateToken(usuarioOpt.get().getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Credenciais inválidas"));
    }

}
