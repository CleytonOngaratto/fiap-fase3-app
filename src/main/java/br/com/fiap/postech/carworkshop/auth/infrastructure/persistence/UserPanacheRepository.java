package br.com.fiap.postech.carworkshop.auth.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserPanacheRepository implements PanacheRepository<UserJpaEntity> {

    public Optional<UserJpaEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
