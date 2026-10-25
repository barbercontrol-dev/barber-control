package com.barbearia.agendamento.service;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.model.Agendamento;
import com.barbearia.agendamento.model.MovimentoFinanceiro;
import com.barbearia.agendamento.repository.AgendamentoRepository;
import com.barbearia.agendamento.repository.MovimentoFinanceiroRepository;
import com.barbearia.agendamento.repository.ServicoRepository;
import com.barbearia.agendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private MovimentoFinanceiroRepository movimentoFinanceiroRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Test
    void cadastraBarbeariaComResponsavelEArmazenaSenhaCodificada() {
        when(usuarioRepository.findByEmail("contato@barbearia.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioService service = criarService();

        Usuario criado = service.cadastrarBarbearia(
                "Barbearia Central", "Ana Souza", "Contato@Barbearia.com", "senha-forte", true);

        assertEquals("Barbearia Central", criado.getNomeBarbearia());
        assertEquals("Ana Souza", criado.getNome());
        assertEquals("contato@barbearia.com", criado.getEmail());
        assertEquals("ADMIN", criado.getRole());
        assertTrue(new BCryptPasswordEncoder().matches("senha-forte", criado.getSenha()));
        assertNull(criado.getBarbearia());
    }

    @Test
    void cadastraBarbeiroAssociadoAoResponsavelSemCredenciaisDeLogin() {
        Usuario responsavel = new Usuario();
        responsavel.setId(17L);
        responsavel.setRole("ADMIN");
        UsuarioService service = criarService();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario barbeiro = service.cadastrarBarbeiro(
                "Carlos Lima", "(11) 99999-9999", "529.982.247-25", responsavel);

        assertEquals("BARBEIRO", barbeiro.getRole());
        assertEquals("Carlos Lima", barbeiro.getNome());
        assertEquals("11999999999", barbeiro.getTelefone());
        assertEquals("52998224725", barbeiro.getCpf());
        assertSame(responsavel, barbeiro.getBarbearia());
        assertTrue(barbeiro.getEmail().startsWith("barbeiro-"));
        assertNotNull(barbeiro.getSenha());
    }

    @Test
    void aceitaCpfSemValidarDigitosVerificadores() {
        Usuario responsavel = new Usuario();
        responsavel.setId(17L);
        UsuarioService service = criarService();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario barbeiro = service.cadastrarBarbeiro(
                "Carlos Lima", "(11) 99999-9999", "111.111.111-11", responsavel);

        assertEquals("11111111111", barbeiro.getCpf());
    }

    @Test
    void atualizaBarbeiroDaBarbeariaSemRejeitarSeuProprioCpf() {
        Usuario barbeiro = new Usuario();
        barbeiro.setId(24L);
        barbeiro.setNome("Carlos Lima");
        barbeiro.setCpf("52998224725");
        when(usuarioRepository.findByIdAndBarbeariaIdAndRole(24L, 17L, "BARBEIRO"))
                .thenReturn(Optional.of(barbeiro));
        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.of(barbeiro));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario atualizado = criarService().atualizarBarbeiro(
                24L, "Carlos Lima Atualizado", "(11) 98888-7777", "529.982.247-25", 17L);

        assertEquals("Carlos Lima Atualizado", atualizado.getNome());
        assertEquals("11988887777", atualizado.getTelefone());
        assertEquals("52998224725", atualizado.getCpf());
    }

    @Test
    void excluiBarbeiroDaBarbeariaMantendoMovimentoFinanceiroSemAgendamento() {
        Usuario barbeiro = new Usuario();
        barbeiro.setId(24L);
        Agendamento agendamento = new Agendamento();
        agendamento.setBarbeiro(barbeiro);
        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setAgendamento(agendamento);

        when(usuarioRepository.findByIdAndBarbeariaIdAndRole(24L, 17L, "BARBEIRO"))
                .thenReturn(Optional.of(barbeiro));
        when(movimentoFinanceiroRepository.findByAgendamento_Barbeiro_Id(24L))
                .thenReturn(java.util.List.of(movimento));
        when(agendamentoRepository.findByBarbeiroId(24L)).thenReturn(java.util.List.of(agendamento));

        boolean excluido = criarService().excluirBarbeiro(24L, 17L);

        assertTrue(excluido);
        assertNull(movimento.getAgendamento());
        verify(movimentoFinanceiroRepository).saveAll(java.util.List.of(movimento));
        verify(agendamentoRepository).deleteAll(java.util.List.of(agendamento));
        verify(usuarioRepository).delete(barbeiro);
    }

    @Test
    void rejeitaCadastroSemConsentimentoDePrivacidade() {
        UsuarioService service = criarService();
        assertThrows(IllegalArgumentException.class, () ->
                service.cadastrarBarbearia("Barbearia Central", "Ana Souza",
                        "contato@barbearia.com", "senha-forte", false));
    }

    private UsuarioService criarService() {
        return new UsuarioService(
                usuarioRepository,
                agendamentoRepository,
                movimentoFinanceiroRepository,
                servicoRepository
        );
    }
}
