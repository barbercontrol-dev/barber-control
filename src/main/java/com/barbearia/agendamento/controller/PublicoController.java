package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.dto.BarbeiroResponse;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import com.barbearia.agendamento.repository.UsuarioRepository;
import com.barbearia.agendamento.service.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rotas públicas, usadas pelo cliente final para agendar.
 * As rotas aqui devolvem dados de outros clientes
 * nem os dados pessoais do barbeiro.
 */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    // Funcionamento da barbearia
    private static final LocalTime ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(20, 0);
    private static final int INTERVALO_MINUTOS = 30;

    // Nome só com letras, espaço, ponto, apóstrofo e hífen. Bloqueia símbolos como < e >
    private static final Pattern NOME_VALIDO = Pattern.compile("^[\\p{L} .'-]{2,100}$");

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public record ServicoPublico(Long id, String nome, BigDecimal valor, Integer duracaoMinutos) {}

    public record AgendarRequest(Long barbeiroId, Long servicoId, String clienteNome,
                                 String clienteTelefone, LocalDateTime dataHora) {}

    @GetMapping("/barbeiros")
    public List<BarbeiroResponse> listarBarbeiros() {
        return usuarioRepository.findByRoleAndBarbeariaIsNotNullOrderByNome("BARBEIRO").stream()
                .map(u -> new BarbeiroResponse(
                        u.getId(),
                        (u.getNome() != null && !u.getNome().isBlank()) ? u.getNome() : "Barbeiro " + u.getId(),
                        u.getBarbearia().getNomeBarbearia()))
                .toList();
    }

    @GetMapping("/servicos")
    public ResponseEntity<?> listarServicos(@RequestParam Long barbeiroId) {
        Usuario barbeiro = usuarioRepository.findByIdComBarbearia(barbeiroId)
                .filter(u -> "BARBEIRO".equals(u.getRole()) && u.getBarbearia() != null)
                .orElse(null);
        if (barbeiro == null) {
            return erro(HttpStatus.BAD_REQUEST, "Barbeiro inválido.");
        }
        return ResponseEntity.ok(servicoRepository.findByBarbeariaIdOrderById(barbeiro.getBarbearia().getId()).stream()
                // ignora registros incompletos (ex: serviço de teste sem nome/valor/duração)
                .filter(s -> s.getNome() != null && s.getValor() != null && s.getDuracaoMinutos() != null)
                .map(s -> new ServicoPublico(s.getId(), s.getNome(), s.getValor(), s.getDuracaoMinutos()))
                .toList());
    }

    @GetMapping("/horarios")
    public ResponseEntity<?> horariosDisponiveis(
            @RequestParam Long barbeiroId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        Usuario barbeiro = usuarioRepository.findByIdComBarbearia(barbeiroId)
                .filter(u -> "BARBEIRO".equals(u.getRole()) && u.getBarbearia() != null)
                .orElse(null);
        if (barbeiro == null || servico == null || servico.getDuracaoMinutos() == null
                || !barbeiro.getBarbearia().getId().equals(servico.getBarbeariaId())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Serviço inválido."));
        }
        int duracao = servico.getDuracaoMinutos();

        List<Agendamento> doDia = agendamentoRepository.findByBarbeiroId(barbeiroId).stream()
                .filter(a -> !"CANCELADO".equals(a.getStatus()))
                .filter(a -> a.getDataHora().toLocalDate().equals(data))
                .toList();

        LocalDateTime agora = LocalDateTime.now();
        List<String> livres = new ArrayList<>();

        for (LocalTime h = ABERTURA; !h.plusMinutes(duracao).isAfter(FECHAMENTO); h = h.plusMinutes(INTERVALO_MINUTOS)) {
            LocalDateTime inicio = data.atTime(h);
            LocalDateTime fim = inicio.plusMinutes(duracao);

            if (inicio.isBefore(agora)) continue;

            boolean ocupado = doDia.stream().anyMatch(a -> {
                LocalDateTime inicioExistente = a.getDataHora();
                LocalDateTime fimExistente = inicioExistente.plusMinutes(duracaoDe(a));
                return inicio.isBefore(fimExistente) && inicioExistente.isBefore(fim);
            });

            if (!ocupado) livres.add(h.toString());
        }

        return ResponseEntity.ok(livres);
    }

    @PostMapping("/agendar")
    public ResponseEntity<?> agendar(@RequestBody AgendarRequest req) {
        if (req.barbeiroId() == null || req.servicoId() == null || req.dataHora() == null
                || req.clienteNome() == null || req.clienteNome().isBlank()
                || req.clienteTelefone() == null) {
            return erro(HttpStatus.BAD_REQUEST, "Preencha todos os campos.");
        }

        String nome = req.clienteNome().trim();
        String telefone = req.clienteTelefone().replaceAll("\\D", "");

        if (!NOME_VALIDO.matcher(nome).matches()) {
            return erro(HttpStatus.BAD_REQUEST, "Nome inválido. Use apenas letras.");
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            return erro(HttpStatus.BAD_REQUEST, "Telefone inválido. Informe DDD + número.");
        }
        if (req.dataHora().isBefore(LocalDateTime.now())) {
            return erro(HttpStatus.BAD_REQUEST, "Esse horário já passou.");
        }

        LocalTime hora = req.dataHora().toLocalTime();
        if (hora.isBefore(ABERTURA) || !hora.isBefore(FECHAMENTO)) {
            return erro(HttpStatus.BAD_REQUEST, "Fora do horário de funcionamento.");
        }

        Usuario barbeiro = usuarioRepository.findByIdComBarbearia(req.barbeiroId())
                .filter(u -> "BARBEIRO".equals(u.getRole()) && u.getBarbearia() != null)
                .orElse(null);
        if (barbeiro == null) {
            return erro(HttpStatus.BAD_REQUEST, "Barbeiro não encontrado.");
        }
        Servico servicoValidado = servicoRepository.findByIdAndBarbeariaId(
                req.servicoId(), barbeiro.getBarbearia().getId()).orElse(null);
        if (servicoValidado == null) {
            return erro(HttpStatus.BAD_REQUEST, "Serviço não encontrado nesta barbearia.");
        }

        Servico servico = new Servico();
        servico.setId(req.servicoId());

        Agendamento agendamento = new Agendamento();
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setClienteNome(nome);
        agendamento.setClienteTelefone(telefone);
        agendamento.setDataHora(req.dataHora());

        try {
            Agendamento criado = agendamentoService.criarAgendamento(agendamento);
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Agendamento confirmado!",
                    "dataHora", criado.getDataHora().toString()));
        } catch (IllegalStateException e) {
            return erro(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return erro(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private int duracaoDe(Agendamento a) {
        if (a.getServicos() != null && !a.getServicos().isEmpty()) {
            return a.getServicos().stream()
                    .map(Servico::getDuracaoMinutos)
                    .filter(duracao -> duracao != null)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        Integer duracao = a.getServico() != null ? a.getServico().getDuracaoMinutos() : null;
        return duracao == null ? 0 : duracao;
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}