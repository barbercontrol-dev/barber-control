package com.barbearia.agendamento.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // WRITE_ONLY: a API aceita a senha na entrada (cadastro/login), mas nunca a devolve nas respostas
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String senha;

    private String nome;

    private String role; // ex: ADMIN, ADMIN gerencia tudo. Como a barbearia só tem administradores, todas as contas criadas terão role = "ADMIN".

    @Column(nullable = false)
    private Boolean consentimentoLgpd = false;

    private LocalDateTime dataConsentimento;
}