package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.dto.BarbeariaRequest;
import com.barbearia.agendamento.dto.BarbeariaResponse;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import com.barbearia.agendamento.service.ContextoUsuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/barbearia")
public class BarbeariaController {

    private static final Pattern FOTO_VALIDA = Pattern.compile("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$");
    private static final int TAMANHO_MAXIMO_FOTO = 2_800_000;

    private final UsuarioRepository usuarioRepository;
    private final ContextoUsuario contexto;

    public BarbeariaController(UsuarioRepository usuarioRepository, ContextoUsuario contexto) {
        this.usuarioRepository = usuarioRepository;
        this.contexto = contexto;
    }

    @GetMapping
    public ResponseEntity<?> buscar(Authentication authentication) {
        var ctx = contexto.de(authentication);
        return usuarioRepository.findById(ctx.barbeariaId())
                .<ResponseEntity<?>>map(b -> ResponseEntity.ok(
                        new BarbeariaResponse(b.getNomeBarbearia(), b.getEndereco(), b.getFoto())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> atualizar(@RequestBody BarbeariaRequest request, Authentication authentication) {
        var ctx = contexto.de(authentication);
        Usuario barbearia = ctx.usuario();

        if (request.endereco() != null) {
            String endereco = request.endereco().trim();
            if (endereco.length() > 255) {
                return ResponseEntity.badRequest().body(Map.of("erro", "O endereço deve ter até 255 caracteres."));
            }
            barbearia.setEndereco(endereco.isEmpty() ? null : endereco);
        }

        if (request.foto() != null) {
            if (request.foto().isEmpty()) {
                barbearia.setFoto(null);
            } else if (request.foto().length() > TAMANHO_MAXIMO_FOTO
                    || !FOTO_VALIDA.matcher(request.foto()).matches()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Foto inválida. Use PNG, JPEG ou WEBP de até 2 MB."));
            } else {
                barbearia.setFoto(request.foto());
            }
        }

        usuarioRepository.save(barbearia);
        return ResponseEntity.ok(new BarbeariaResponse(
                barbearia.getNomeBarbearia(), barbearia.getEndereco(), barbearia.getFoto()));
    }
}