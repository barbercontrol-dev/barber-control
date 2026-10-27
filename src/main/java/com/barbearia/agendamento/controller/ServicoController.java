package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.service.ContextoUsuario;
import com.barbearia.agendamento.service.ServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private static final Pattern FOTO_VALIDA = Pattern.compile("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$");
    private static final int TAMANHO_MAXIMO_FOTO = 2_800_000;

    @Autowired
    private ServicoService servicoService;

    @Autowired
    private ContextoUsuario contexto;

    @PostMapping
    public ResponseEntity<?> criarServico(@RequestBody Servico servico, Authentication authentication) {
        if (!fotoValida(servico.getFoto())) {
            return erroFoto();
        }
        return ResponseEntity.ok(
                servicoService.criarServico(servico, contexto.de(authentication).barbeariaId()));
    }

    @GetMapping
    public List<Servico> listarServicos(Authentication authentication) {
        return servicoService.listarTodos(contexto.de(authentication).barbeariaId());
    }

    @GetMapping("/{id}")
    public Optional<Servico> buscarServicoPorId(@PathVariable Long id, Authentication authentication) {
        return servicoService.buscarPorId(id, contexto.de(authentication).barbeariaId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarServico(@PathVariable Long id, @RequestBody Servico servicoAtualizado,
                                               Authentication authentication) {
        Long barbeariaId = contexto.de(authentication).barbeariaId();
        Optional<Servico> existente = servicoService.buscarPorId(id, barbeariaId);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (servicoAtualizado.getFoto() != null && !fotoValida(servicoAtualizado.getFoto())) {
            return erroFoto();
        }
        if (servicoAtualizado.getFoto() == null && existente.get().getFoto() == null) {
            return erroFoto();
        }
        return ResponseEntity.ok(servicoService.atualizarServico(id, servicoAtualizado, barbeariaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarServico(@PathVariable Long id, Authentication authentication) {
        return servicoService.deletarServico(id, contexto.de(authentication).barbeariaId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private boolean fotoValida(String foto) {
        return foto != null && foto.length() <= TAMANHO_MAXIMO_FOTO && FOTO_VALIDA.matcher(foto).matches();
    }

    private ResponseEntity<Map<String, String>> erroFoto() {
        return ResponseEntity.badRequest().body(Map.of(
                "erro", "A foto é obrigatória. Envie uma imagem PNG, JPEG ou WEBP de até 2 MB."));
    }
}