package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTests {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private FinanceiroService financeiroService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    @Test
    void salvaTodosOsServicosSelecionadosNoAgendamento() {
        Usuario responsavel = responsavel();
        Usuario barbeiro = barbeiro(responsavel);
        Servico corte = servico(1L, "Corte", 30);
        Servico barba = servico(2L, "Barba", 30);
        when(servicoRepository.findByIdAndBarbeariaId(1L, responsavel.getId()))
                .thenReturn(Optional.of(corte));
        when(servicoRepository.findByIdAndBarbeariaId(2L, responsavel.getId()))
                .thenReturn(Optional.of(barba));
        when(agendamentoRepository.findByBarbeiroId(barbeiro.getId())).thenReturn(List.of());
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Agendamento pedido = novoAgendamento(barbeiro, List.of(corte.getId(), barba.getId()));

        Agendamento salvo = agendamentoService.criarAgendamento(pedido);

        assertEquals(List.of(corte, barba), salvo.getServicos());
        assertEquals(corte, salvo.getServico());
        verify(agendamentoRepository).save(pedido);
    }

    @Test
    void consideraDuracaoSomadaAoVerificarConflitos() {
        Usuario responsavel = responsavel();
        Usuario barbeiro = barbeiro(responsavel);
        Servico corte = servico(1L, "Corte", 30);
        Servico barba = servico(2L, "Barba", 30);
        when(servicoRepository.findByIdAndBarbeariaId(1L, responsavel.getId()))
                .thenReturn(Optional.of(corte));
        when(servicoRepository.findByIdAndBarbeariaId(2L, responsavel.getId()))
            .thenReturn(Optional.of(barba));
        Agendamento existente = new Agendamento();
        existente.setId(10L);
        existente.setBarbeiro(barbeiro);
        existente.setServicos(List.of(corte, barba));
        existente.setServico(corte);
        existente.setDataHora(LocalDateTime.now().plusDays(1).withHour(10).withMinute(45));
        existente.setStatus("AGENDADO");
        when(agendamentoRepository.findByBarbeiroId(barbeiro.getId()))
                .thenReturn(List.of(existente));

        Agendamento pedido = novoAgendamento(barbeiro, List.of(corte.getId(), barba.getId()));
        pedido.setDataHora(existente.getDataHora().minusMinutes(45));

        assertThrows(IllegalStateException.class, () -> agendamentoService.criarAgendamento(pedido));
    }

    private Agendamento novoAgendamento(Usuario barbeiro, List<Long> servicoIds) {
        Agendamento agendamento = new Agendamento();
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServicos(servicoIds.stream().map(id -> {
            Servico servico = new Servico();
            servico.setId(id);
            return servico;
        }).toList());
        agendamento.setClienteNome("Cliente teste");
        agendamento.setDataHora(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        return agendamento;
    }

    private Usuario responsavel() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        return usuario;
    }

    private Usuario barbeiro(Usuario responsavel) {
        Usuario barbeiro = new Usuario();
        barbeiro.setId(8L);
        barbeiro.setBarbearia(responsavel);
        return barbeiro;
    }

    private Servico servico(Long id, String nome, int duracao) {
        return new Servico(id, nome, new BigDecimal("10.00"), duracao);
    }
}