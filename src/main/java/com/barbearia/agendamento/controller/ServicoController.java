package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.service.ServicoService;
import com.barbearia.agendamento.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servicos")

public class ServicoController {

    @Autowired
    private ServicoService servicoService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public Servico criarServico(@RequestBody Servico servico, Authentication authentication) {
        Long barbeariaId = usuarioService.buscarResponsavel(authentication.getName()).getId();
        return servicoService.criarServico(servico, barbeariaId);
    }


    @GetMapping
    public List<Servico> listarServicos(Authentication authentication) {
        Long barbeariaId = usuarioService.buscarResponsavel(authentication.getName()).getId();
        return servicoService.listarTodos(barbeariaId);
    }


    @GetMapping("/{id}")
    public Optional<Servico> buscarServicoPorId(@PathVariable Long id, Authentication authentication) {
        Long barbeariaId = usuarioService.buscarResponsavel(authentication.getName()).getId();
        return servicoService.buscarPorId(id, barbeariaId);
    }


    @PutMapping("/{id}")
    public Servico atualizarServico(@PathVariable Long id, @RequestBody Servico servicoAtualizado,
                                    Authentication authentication) {
        Long barbeariaId = usuarioService.buscarResponsavel(authentication.getName()).getId();
        return servicoService.atualizarServico(id, servicoAtualizado, barbeariaId);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarServico(@PathVariable Long id, Authentication authentication) {
        Long barbeariaId = usuarioService.buscarResponsavel(authentication.getName()).getId();
        return servicoService.deletarServico(id, barbeariaId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
