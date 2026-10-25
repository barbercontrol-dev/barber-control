package com.barbearia.agendamento;

import com.barbearia.agendamento.model.Usuario;
import com.barbearia.agendamento.repository.UsuarioRepository;
import com.barbearia.agendamento.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class BarbeiroAuthorizationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void permiteCadastroDeBarbeiroParaResponsavelAutenticado() throws Exception {
        Usuario responsavel = new Usuario();
        responsavel.setNome("Responsável de teste");
        responsavel.setNomeBarbearia("Barbearia de teste");
        responsavel.setEmail("teste-" + System.nanoTime() + "@barbearia.com");
        responsavel.setSenha(passwordEncoder.encode("senha-forte"));
        responsavel.setRole("ADMIN");
        responsavel.setConsentimentoLgpd(true);
        responsavel.setConsentimentoLgpd(true);
        responsavel = usuarioRepository.saveAndFlush(responsavel);

        mockMvc.perform(post("/api/barbeiros")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(responsavel.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Barbeiro de teste",
                                  "telefone": "(11) 99999-9999",
                                  "cpf": "111.111.111-11"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void solicitaLoginQuandoCadastroDeBarbeiroNaoTemToken() throws Exception {
        mockMvc.perform(post("/api/barbeiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Barbeiro de teste",
                                  "telefone": "(11) 99999-9999",
                                  "cpf": "111.111.111-11"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
