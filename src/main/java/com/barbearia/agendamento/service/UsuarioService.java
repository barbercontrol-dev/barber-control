package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.model.MovimentoFinanceiro;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.MovimentoFinanceiroRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final MovimentoFinanceiroRepository movimentoFinanceiroRepository;
    private final ServicoRepository servicoRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            AgendamentoRepository agendamentoRepository,
            MovimentoFinanceiroRepository movimentoFinanceiroRepository,
            ServicoRepository servicoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.movimentoFinanceiroRepository = movimentoFinanceiroRepository;
        this.servicoRepository = servicoRepository;
    }

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public Usuario cadastrarBarbearia(String nomeBarbearia, String nomeResponsavel, String email,
                                     String senha, Boolean consentimentoLgpd) {
        if (consentimentoLgpd == null || !consentimentoLgpd) {
            throw new IllegalArgumentException("É necessário aceitar os termos de uso e a política de privacidade para se cadastrar.");
        }
        String nomeValidado = validarNome(nomeResponsavel, "Informe o nome do responsável.");
        String barbeariaValidada = validarNome(nomeBarbearia, "Informe o nome da barbearia.");
        String emailValidado = email == null ? "" : email.trim().toLowerCase();
        if (!emailValidado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || usuarioRepository.findByEmail(emailValidado).isPresent()) {
            throw new IllegalArgumentException("Este email já está cadastrado ou é inválido.");
        }
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }

        Usuario responsavel = new Usuario();
        responsavel.setNome(nomeValidado);
        responsavel.setNomeBarbearia(barbeariaValidada);
        responsavel.setEmail(emailValidado);
        responsavel.setSenha(encoder.encode(senha));
        responsavel.setRole("ADMIN");
        responsavel.setConsentimentoLgpd(true);
        responsavel.setDataConsentimento(LocalDateTime.now());
        return usuarioRepository.save(responsavel);
    }

    public List<Usuario> listarBarbeiros(Long barbeariaId) {
        return usuarioRepository.findByBarbeariaIdAndRoleOrderByNome(barbeariaId, "BARBEIRO");
    }

    public Usuario cadastrarBarbeiro(String nome, String telefone, String cpf, Usuario responsavel) {
        Usuario barbeiro = new Usuario();
        barbeiro.setNome(validarNome(nome, "Informe o nome do barbeiro."));
        barbeiro.setEmail("barbeiro-" + UUID.randomUUID() + "@interno.barbercontrol.local");
        barbeiro.setSenha(encoder.encode(UUID.randomUUID().toString()));
        barbeiro.setTelefone(validarTelefone(telefone));
        barbeiro.setCpf(validarCpf(cpf, null));
        barbeiro.setRole("BARBEIRO");
        barbeiro.setConsentimentoLgpd(false);
        barbeiro.setBarbearia(responsavel);
        return usuarioRepository.save(barbeiro);
    }

    public Usuario atualizarBarbeiro(Long barbeiroId, String nome, String telefone, String cpf, Long barbeariaId) {
        Usuario barbeiro = buscarBarbeiroDaBarbearia(barbeiroId, barbeariaId);
        barbeiro.setNome(validarNome(nome, "Informe o nome do barbeiro."));
        barbeiro.setTelefone(validarTelefone(telefone));
        barbeiro.setCpf(validarCpf(cpf, barbeiroId));
        return usuarioRepository.save(barbeiro);
    }

    @Transactional
    public boolean excluirBarbeiro(Long barbeiroId, Long barbeariaId) {
        Optional<Usuario> resultado = usuarioRepository.findByIdAndBarbeariaIdAndRole(
                barbeiroId, barbeariaId, "BARBEIRO");
        if (resultado.isEmpty()) {
            return false;
        }

        List<MovimentoFinanceiro> movimentos =
                movimentoFinanceiroRepository.findByAgendamento_Barbeiro_Id(barbeiroId);
        movimentos.forEach(movimento -> movimento.setAgendamento(null));
        movimentoFinanceiroRepository.saveAll(movimentos);

        agendamentoRepository.deleteAll(agendamentoRepository.findByBarbeiroId(barbeiroId));
        usuarioRepository.delete(resultado.get());
        return true;
    }

    public Usuario buscarBarbeiroDaBarbearia(Long barbeiroId, Long barbeariaId) {
        return usuarioRepository.findByIdAndBarbeariaIdAndRole(barbeiroId, barbeariaId, "BARBEIRO")
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro não encontrado nesta barbearia."));
    }

    private String validarTelefone(String telefone) {
        String telefoneValidado = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (telefoneValidado.length() < 10 || telefoneValidado.length() > 11) {
            throw new IllegalArgumentException("Informe um telefone válido com DDD.");
        }
        return telefoneValidado;
    }

    private String validarCpf(String cpf, Long barbeiroId) {
        String cpfValidado = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (!cpfValidado.isBlank()) {
            Optional<Usuario> existente = usuarioRepository.findByCpf(cpfValidado);
            if (existente.isPresent() && !existente.get().getId().equals(barbeiroId)) {
                throw new IllegalArgumentException("Este CPF já está cadastrado.");
            }
        }
        return cpfValidado;
    }

    public Usuario buscarResponsavel(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
        if (!"ADMIN".equals(usuario.getRole())) {
            throw new IllegalArgumentException("Acesso permitido somente ao responsável da barbearia.");
        }
        return usuario;
    }

    private String validarNome(String nome, String mensagemErro) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 120) {
            throw new IllegalArgumentException(mensagemErro);
        }
        return nome.trim();
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean validarSenha(String senhaDigitada, String senhaArmazenada) {
        return senhaDigitada != null && senhaArmazenada != null
                && encoder.matches(senhaDigitada, senhaArmazenada);
    }

    // Direito de exclusão: apaga os agendamentos do barbeiro
    // e depois a conta. Tudo na mesma transação: ou apaga tudo, ou nada.
    @Transactional
    public void excluirConta(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        movimentoFinanceiroRepository.deleteAllByUsuarioId(usuario.getId());
        List<Usuario> barbeiros = listarBarbeiros(usuario.getId());
        for (Usuario barbeiro : barbeiros) {
            agendamentoRepository.deleteAll(agendamentoRepository.findByBarbeiroId(barbeiro.getId()));
        }
        agendamentoRepository.deleteAll(agendamentoRepository.findByBarbeiroId(usuario.getId()));
        servicoRepository.deleteAllByBarbeariaId(usuario.getId());
        usuarioRepository.deleteAll(barbeiros);
        usuarioRepository.delete(usuario);
    }
}