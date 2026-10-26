package com.barbearia.agendamento.dto;

import java.time.LocalDateTime;

public record BloqueioResponse(Long id, Long barbeiroId, LocalDateTime inicio, LocalDateTime fim) {
}