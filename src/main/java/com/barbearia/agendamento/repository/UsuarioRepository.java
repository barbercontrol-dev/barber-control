package com.barbearia.agendamento.repository;

import com.barbearia.agendamento.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    List<Usuario> findByBarbeariaIdAndRoleOrderByNome(Long barbeariaId, String role);

    Optional<Usuario> findByIdAndBarbeariaIdAndRole(Long id, Long barbeariaId, String role);

    @EntityGraph(attributePaths = "barbearia")
    List<Usuario> findByRoleAndBarbeariaIsNotNullOrderByNome(String role);

    @EntityGraph(attributePaths = "barbearia")
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdComBarbearia(@Param("id") Long id);
}
