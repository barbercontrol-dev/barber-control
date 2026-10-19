package com.barbearia.agendamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovimentoFinanceiroResponse(
        Long id,
        String tipo,
        LocalDate data,
        String nomeCliente,
        String descricao,
        String categoria,
        String formaPagamento,
        BigDecimal valor,
        String usuarioNome,
        Long agendamentoId,
        List<ServicoRegistrado> servicos
) {
    public record ServicoRegistrado(Long id, String nome, BigDecimal valor) {
    }
}
