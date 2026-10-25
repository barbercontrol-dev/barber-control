package com.barbearia.agendamento.dto;

public record RegistroBarbeariaRequest(
        String nomeBarbearia,
        String nomeResponsavel,
        String email,
        String senha,
        Boolean consentimentoLgpd
) {
}
