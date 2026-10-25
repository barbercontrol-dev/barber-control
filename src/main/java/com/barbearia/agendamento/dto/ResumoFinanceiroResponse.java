package com.barbearia.agendamento.dto;

import java.math.BigDecimal;

public record ResumoFinanceiroResponse(
        BigDecimal faturamento,
        BigDecimal gastos,
        BigDecimal lucro,
        long atendimentos
) {
}
