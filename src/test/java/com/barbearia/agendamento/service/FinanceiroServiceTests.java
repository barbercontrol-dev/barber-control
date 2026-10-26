package com.barbearia.agendamento.service;

import com.barbearia.agendamento.dto.MovimentoFinanceiroRequest;
import com.barbearia.agendamento.dto.MovimentoFinanceiroResponse;
import com.barbearia.agendamento.dto.ResumoFinanceiroResponse;
import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.MovimentoFinanceiro;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.MovimentoFinanceiroRepository;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTests {

    @Mock
    private MovimentoFinanceiroRepository movimentoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private FinanceiroService financeiroService;

    @BeforeEach
    void setUp() {
        financeiroService = new FinanceiroService(movimentoRepository, servicoRepository, agendamentoRepository);
    }

    @Test
    void registraEntradaComTotalCalculadoDosServicosPersistidos() {
        Servico corte = new Servico(1L, "Corte", new BigDecimal("45.00"), 30);
        Servico barba = new Servico(2L, "Barba", new BigDecimal("25.00"), 20);
        corte.setBarbeariaId(5L);
        barba.setBarbeariaId(5L);
        when(servicoRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(corte, barba));
        when(movimentoRepository.save(any(MovimentoFinanceiro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNome("Alex");

        MovimentoFinanceiroRequest request = new MovimentoFinanceiroRequest(
                "ENTRADA",
                LocalDate.of(2026, 10, 19),
                "Cliente",
                null,
                null,
                "PIX",
                new BigDecimal("1.00"),
                List.of(1L, 2L)
        );

        MovimentoFinanceiroResponse resposta = financeiroService.registrar(request, usuario);
        ArgumentCaptor<MovimentoFinanceiro> movimentoCaptor = ArgumentCaptor.forClass(MovimentoFinanceiro.class);
        verify(movimentoRepository).save(movimentoCaptor.capture());

        assertEquals(new BigDecimal("70.00"), resposta.valor());
        assertEquals("Alex", resposta.usuarioNome());
        assertEquals(2, resposta.servicos().size());
        assertEquals(new BigDecimal("70.00"), movimentoCaptor.getValue().getValor());
    }

    @Test
    void rejeitaFormaDePagamentoForaDasOpcoesPermitidas() {
        MovimentoFinanceiroRequest request = new MovimentoFinanceiroRequest(
                "SAIDA",
                LocalDate.of(2026, 10, 19),
                null,
                "Conta de luz",
                "Contas",
                "BOLETO",
                new BigDecimal("120.00"),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> financeiroService.registrar(request, new Usuario()));
        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void registraSaidaComCategoriaEValorInformados() {
        when(movimentoRepository.save(any(MovimentoFinanceiro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        Usuario usuario = new Usuario();
        usuario.setNome("Alex");
        MovimentoFinanceiroRequest request = new MovimentoFinanceiroRequest(
                "SAIDA",
                LocalDate.of(2026, 10, 19),
                null,
                "Compra de produtos",
                "Produtos",
                "DEBITO",
                new BigDecimal("189.90"),
                null
        );

        MovimentoFinanceiroResponse resposta = financeiroService.registrar(request, usuario);

        assertEquals(new BigDecimal("189.90"), resposta.valor());
        assertEquals("Produtos", resposta.categoria());
        assertEquals("Compra de produtos", resposta.descricao());
    }

    @Test
    void criaUmaEntradaVinculadaAoAgendamentoConcluido() {
        Servico corte = new Servico(12L, "Corte", new BigDecimal("45.00"), 30);
                Servico barba = new Servico(13L, "Barba", new BigDecimal("25.00"), 20);
        Usuario responsavel = new Usuario();
        responsavel.setId(21L);
        responsavel.setNome("Barbearia Central");
        Usuario barbeiro = new Usuario();
        barbeiro.setNome("Alex");
        barbeiro.setBarbearia(responsavel);
        Agendamento agendamento = new Agendamento(
                37L,
                barbeiro,
                corte,
                "Cliente",
                null,
                LocalDate.of(2026, 10, 19).atTime(14, 30),
                "AGENDADO"
        );
        agendamento.setServicos(List.of(corte, barba));
        when(movimentoRepository.findByAgendamentoId(37L)).thenReturn(java.util.Optional.empty());
        when(movimentoRepository.save(any(MovimentoFinanceiro.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        financeiroService.registrarEntradaAgendamento(agendamento, "PIX");

        ArgumentCaptor<MovimentoFinanceiro> movimentoCaptor = ArgumentCaptor.forClass(MovimentoFinanceiro.class);
        verify(movimentoRepository).save(movimentoCaptor.capture());
        MovimentoFinanceiro movimento = movimentoCaptor.getValue();
        assertEquals("ENTRADA", movimento.getTipo());
        assertEquals("PIX", movimento.getFormaPagamento());
        assertEquals(new BigDecimal("70.00"), movimento.getValor());
        assertEquals(responsavel, movimento.getUsuario());
        assertEquals(agendamento, movimento.getAgendamento());
        assertEquals(12L, movimento.getServicos().get(0).getServicoId());
        assertEquals(13L, movimento.getServicos().get(1).getServicoId());
    }

    @Test
    void naoDuplicaEntradaDoMesmoAgendamento() {
        Servico corte = new Servico(12L, "Corte", new BigDecimal("45.00"), 30);
        Agendamento agendamento = new Agendamento(
                37L,
                new Usuario(),
                corte,
                "Cliente",
                null,
                LocalDate.of(2026, 10, 19).atTime(14, 30),
                "CONCLUIDO"
        );
        when(movimentoRepository.findByAgendamentoId(37L))
                .thenReturn(java.util.Optional.of(new MovimentoFinanceiro()));

        financeiroService.registrarEntradaAgendamento(agendamento, "PIX");

        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void resumoIncluiAtendimentosConcluidosSemDuplicarEntradasVinculadas() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);

        Servico corte = new Servico(12L, "Corte", new BigDecimal("50.00"), 30);
        Agendamento agendamento = new Agendamento(
                37L,
                new Usuario(),
                corte,
                "Cliente",
                null,
                LocalDate.of(2026, 10, 19).atTime(14, 30),
                "CONCLUIDO"
        );
        MovimentoFinanceiro entradaVinculada = new MovimentoFinanceiro();
        entradaVinculada.setValor(new BigDecimal("45.00"));
        entradaVinculada.setAgendamento(agendamento);
        MovimentoFinanceiro entradaManual = new MovimentoFinanceiro();
        entradaManual.setValor(new BigDecimal("12.00"));
        MovimentoFinanceiro gasto = new MovimentoFinanceiro();
        gasto.setValor(new BigDecimal("10.00"));

        when(movimentoRepository.findByUsuarioIdAndTipoAndDataBetweenOrderByDataDescIdDesc(
                21L, "ENTRADA", inicio, fim)).thenReturn(List.of(entradaVinculada, entradaManual));
        when(movimentoRepository.findByUsuarioIdAndTipoAndDataBetweenOrderByDataDescIdDesc(
                21L, "SAIDA", inicio, fim)).thenReturn(List.of(gasto));
        when(agendamentoRepository.findConcluidosDaBarbearia(
                21L, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(agendamento));

        ResumoFinanceiroResponse resumo = financeiroService.resumir(21L, inicio, fim);

        assertEquals(new BigDecimal("57.00"), resumo.faturamento());
        assertEquals(new BigDecimal("10.00"), resumo.gastos());
        assertEquals(new BigDecimal("47.00"), resumo.lucro());
        assertEquals(2, resumo.atendimentos());
    }
}
