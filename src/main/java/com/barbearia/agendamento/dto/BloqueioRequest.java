package com.barbearia.agendamento.dto;

import java.time.LocalDateTime;

public record BloqueioRequest(BarbeiroRef barbeiro, LocalDateTime inicio, LocalDateTime fim) {
    public record BarbeiroRef(Long id) {
    }
}