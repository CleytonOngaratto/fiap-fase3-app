package br.com.fiap.postech.carworkshop.auth.usecase.port.out;

import br.com.fiap.postech.carworkshop.auth.domain.entity.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByUsername(String username);
    User save(User user);
}
