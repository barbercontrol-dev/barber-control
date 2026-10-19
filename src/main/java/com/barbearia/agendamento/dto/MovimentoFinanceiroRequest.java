package com.barbearia.agendamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovimentoFinanceiroRequest(
        String tipo,
        LocalDate data,
        String nomeCliente,
        String descricao,
        String categoria,
        String formaPagamento,
        BigDecimal valor,
        List<Long> servicoIds
) {
}
