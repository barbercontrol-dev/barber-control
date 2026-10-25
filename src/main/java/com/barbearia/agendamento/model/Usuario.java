package com.barbearia.agendamento.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    // WRITE_ONLY: a API aceita a senha na entrada (cadastro/login), mas nunca a devolve nas respostas
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    private String nome;

    @Column(length = 20, unique = true)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    private String nomeBarbearia;

    private String role;

    private Boolean consentimentoLgpd;

    private LocalDateTime dataConsentimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbearia_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario barbearia;
}