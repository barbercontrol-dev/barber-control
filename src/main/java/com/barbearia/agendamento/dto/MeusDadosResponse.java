package com.barbearia.agendamento.dto;

public record MeusDadosResponse(
        Long id,
        String nome,
        String email,
        String perfil,
        String nomeBarbearia,
        String endereco,
        Long barbeiroId
) {
}