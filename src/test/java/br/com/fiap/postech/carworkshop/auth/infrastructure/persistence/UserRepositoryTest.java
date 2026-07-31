package br.com.fiap.postech.carworkshop.auth.infrastructure.persistence;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserRepositoryTest {

    @Inject
    UserPanacheRepository userRepository;

    // Isolamento: @TestTransaction reverte tudo ao final; o deleteAll() (clean slate) roda dentro da
    // transação e é desfeito no rollback, preservando o seed compartilhado para as outras classes.

    @Test
    @TestTransaction
    void testFindByUsername_should_return_user_when_username_exists() {
        userRepository.deleteAll();
        var user = UserJpaEntity.builder()
                .username("admin").password("hashedPassword123").salt("salt123")
                .roles(List.of()).build();
        userRepository.persist(user);

        var result = userRepository.findByUsername("admin");

        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
        assertEquals("hashedPassword123", result.get().getPassword());
    }

    @Test
    @TestTransaction
    void testFindByUsername_should_return_empty_when_username_does_not_exist() {
        userRepository.deleteAll();
        assertTrue(userRepository.findByUsername("nonexistent").isEmpty());
    }

    @Test
    @TestTransaction
    void testPersistAndFindById_should_work_correctly() {
        userRepository.deleteAll();
        var user = UserJpaEntity.builder()
                .username("user1").password("pass123").salt("salt456")
                .roles(List.of()).build();
        userRepository.persist(user);

        var result = userRepository.findByIdOptional(user.id);

        assertTrue(result.isPresent());
        assertEquals("user1", result.get().getUsername());
    }
}
