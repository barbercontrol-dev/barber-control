package com.barbearia.agendamento.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Usuario barbeiro;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

        @ManyToMany
        @JoinTable(
            name = "agendamento_servicos",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
        )
        private List<Servico> servicos = new ArrayList<>();

    private String clienteNome;

    private String clienteTelefone;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    private String status; // AGENDADO, CONCLUIDO, CANCELADO

    public Agendamento(
            Long id,
            Usuario barbeiro,
            Servico servico,
            String clienteNome,
            String clienteTelefone,
            LocalDateTime dataHora,
            String status
    ) {
        this.id = id;
        this.barbeiro = barbeiro;
        this.servico = servico;
        this.clienteNome = clienteNome;
        this.clienteTelefone = clienteTelefone;
        this.dataHora = dataHora;
        this.status = status;
    }
}