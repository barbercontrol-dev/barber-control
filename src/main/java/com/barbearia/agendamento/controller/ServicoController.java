package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.service.ServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servicos")

public class ServicoController {

    @Autowired
    private ServicoService servicoService;


    @PostMapping
    public Servico criarServico(@RequestBody Servico servico) {
        return servicoService.criarServico(servico);
    }


    @GetMapping
    public List<Servico> listarServicos() {
        return servicoService.listarTodos();
    }


    @GetMapping("/{id}")
    public Optional<Servico> buscarServicoPorId(@PathVariable Long id) {
        return servicoService.buscarPorId(id);
    }


    @PutMapping("/{id}")
    public Servico atualizarServico(@PathVariable Long id, @RequestBody Servico servicoAtualizado) {
        return servicoService.atualizarServico(id, servicoAtualizado);
    }


    @DeleteMapping("/{id}")
    public void deletarServico(@PathVariable Long id) {
        servicoService.deletarServico(id);
    }
}
