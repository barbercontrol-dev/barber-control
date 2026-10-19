package com.barbearia.agendamento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movimento_financeiro")
@Getter
@Setter
@NoArgsConstructor
public class MovimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 120)
    private String nomeCliente;

    @Column(length = 180)
    private String descricao;

    @Column(length = 80)
    private String categoria;

    @Column(nullable = false, length = 20)
    private String formaPagamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true)
    private Agendamento agendamento;

    @OneToMany(mappedBy = "movimento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ServicoMovimentoFinanceiro> servicos = new ArrayList<>();

    public void adicionarServico(ServicoMovimentoFinanceiro servico) {
        servicos.add(servico);
        servico.setMovimento(this);
    }
}
