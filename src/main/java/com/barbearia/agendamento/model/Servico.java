package com.barbearia.agendamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Servico {

    public Servico(Long id, String nome, BigDecimal valor, Integer duracaoMinutos) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.duracaoMinutos = duracaoMinutos;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private BigDecimal valor;

    private Integer duracaoMinutos;

    @Column(name = "barbearia_id")
    private Long barbeariaId;

    @Lob
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String foto;
}
