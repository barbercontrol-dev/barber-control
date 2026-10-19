package com.barbearia.agendamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtendimentoPendenteFinanceiroResponse(
        Long id,
        LocalDate data,
        String nomeCliente,
        String servicoNome,
        BigDecimal valor
) {
}
