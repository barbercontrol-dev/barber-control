package com.barbearia.agendamento.dto;

public record BarbeiroRequest(String nome, String email, String telefone, String cpf, String senha) {
}