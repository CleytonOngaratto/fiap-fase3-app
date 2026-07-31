package br.com.fiap.postech.carworkshop.auth.adapter.gateway;

import br.com.fiap.postech.carworkshop.auth.domain.entity.User;
import br.com.fiap.postech.carworkshop.auth.infrastructure.mapper.UserJpaMapper;
import br.com.fiap.postech.carworkshop.auth.infrastructure.persistence.UserPanacheRepository;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class UserRepositoryGateway implements UserRepositoryPort {

    @Inject
    UserPanacheRepository repository;

    @Inject
    UserJpaMapper mapper;

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        var entity = mapper.toJpaEntity(user);
        repository.persistAndFlush(entity);
        return mapper.toDomain(entity);
    }
}
