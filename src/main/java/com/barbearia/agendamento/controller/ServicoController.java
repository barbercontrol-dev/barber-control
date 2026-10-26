package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.service.ContextoUsuario;
import com.barbearia.agendamento.service.ServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    @Autowired
    private ServicoService servicoService;

    @Autowired
    private ContextoUsuario contexto;

    @PostMapping
    public Servico criarServico(@RequestBody Servico servico, Authentication authentication) {
        return servicoService.criarServico(servico, contexto.de(authentication).barbeariaId());
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
    public Servico atualizarServico(@PathVariable Long id, @RequestBody Servico servicoAtualizado,
                                    Authentication authentication) {
        return servicoService.atualizarServico(id, servicoAtualizado, contexto.de(authentication).barbeariaId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarServico(@PathVariable Long id, Authentication authentication) {
        return servicoService.deletarServico(id, contexto.de(authentication).barbeariaId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}