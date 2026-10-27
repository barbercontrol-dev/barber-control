package com.barbearia.agendamento.controller;

import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.Servico;
import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.dto.BarbeariaResponse;
import com.barbearia.agendamento.dto.BarbeiroResponse;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.BloqueioRepository;
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

@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private static final LocalTime ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(20, 0);
    private static final int INTERVALO_MINUTOS = 30;
    private static final Pattern NOME_VALIDO = Pattern.compile("^[\\p{L} .'-]{2,100}$");

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BloqueioRepository bloqueioRepository;

    public record ServicoPublico(Long id, String nome, BigDecimal valor, Integer duracaoMinutos, String foto) {}

    public record AgendarRequest(Long barbeiroId, Long servicoId, List<Long> servicoIds, String clienteNome,
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

    @GetMapping("/barbearia")
    public ResponseEntity<?> barbearia(@RequestParam Long barbeiroId) {
        Usuario barbeiro = usuarioRepository.findByIdComBarbearia(barbeiroId)
                .filter(u -> "BARBEIRO".equals(u.getRole()) && u.getBarbearia() != null)
                .orElse(null);
        if (barbeiro == null) {
            return erro(HttpStatus.BAD_REQUEST, "Barbeiro inválido.");
        }
        Usuario b = barbeiro.getBarbearia();
        return ResponseEntity.ok(new BarbeariaResponse(b.getNomeBarbearia(), b.getEndereco(), b.getFoto()));
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
                .filter(s -> s.getNome() != null && s.getValor() != null && s.getDuracaoMinutos() != null)
                .map(s -> new ServicoPublico(
                        s.getId(), s.getNome(), s.getValor(), s.getDuracaoMinutos(), s.getFoto()))
                .toList());
    }

    @GetMapping("/horarios")
    public ResponseEntity<?> horariosDisponiveis(
            @RequestParam Long barbeiroId,
            @RequestParam(required = false) Long servicoId,
            @RequestParam(required = false) List<Long> servicoIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        Usuario barbeiro = usuarioRepository.findByIdComBarbearia(barbeiroId)
                .filter(u -> "BARBEIRO".equals(u.getRole()) && u.getBarbearia() != null)
                .orElse(null);
        List<Long> ids = resolverServicoIds(servicoId, servicoIds);
        if (barbeiro == null || ids.isEmpty()
                || ids.stream().anyMatch(id -> id == null || id <= 0)
                || ids.stream().distinct().count() != ids.size()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Serviço inválido."));
        }
        List<Servico> servicos = ids.stream()
                .map(id -> servicoRepository.findByIdAndBarbeariaId(id, barbeiro.getBarbearia().getId())
                        .orElse(null))
                .toList();
        if (servicos.stream().anyMatch(servico ->
                servico == null || servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() <= 0)) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Serviço inválido."));
        }
        int duracao = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum();

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
            if (bloqueioRepository.existeConflito(barbeiroId, inicio, fim)) continue;

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
        List<Long> servicoIds = resolverServicoIds(req.servicoId(), req.servicoIds());
        if (req.barbeiroId() == null || servicoIds.isEmpty() || req.dataHora() == null
                || req.clienteNome() == null || req.clienteNome().isBlank()
                || req.clienteTelefone() == null) {
            return erro(HttpStatus.BAD_REQUEST, "Preencha todos os campos.");
        }
        if (servicoIds.stream().anyMatch(id -> id == null || id <= 0)
                || servicoIds.stream().distinct().count() != servicoIds.size()) {
            return erro(HttpStatus.BAD_REQUEST, "A seleção de serviços é inválida.");
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
        List<Servico> servicosValidados = servicoIds.stream()
                .map(id -> servicoRepository.findByIdAndBarbeariaId(id, barbeiro.getBarbearia().getId())
                        .orElse(null))
                .toList();
        if (servicosValidados.stream().anyMatch(servico -> servico == null
                || servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() <= 0)) {
            return erro(HttpStatus.BAD_REQUEST, "Um ou mais serviços não foram encontrados nesta barbearia.");
        }

        int duracao = servicosValidados.stream().mapToInt(Servico::getDuracaoMinutos).sum();
        if (bloqueioRepository.existeConflito(barbeiro.getId(), req.dataHora(), req.dataHora().plusMinutes(duracao))) {
            return erro(HttpStatus.CONFLICT, "Esse horário não está mais disponível.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServicos(servicoIds.stream().map(id -> {
            Servico servico = new Servico();
            servico.setId(id);
            return servico;
        }).toList());
        agendamento.setServico(agendamento.getServicos().get(0));
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

    private List<Long> resolverServicoIds(Long servicoId, List<Long> servicoIds) {
        if (servicoIds != null && !servicoIds.isEmpty()) {
            return servicoIds;
        }
        return servicoId == null ? List.of() : List.of(servicoId);
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}