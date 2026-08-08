package br.com.fiap.postech.carworkshop.autoservice.adapter.gateway;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.autoservice.infrastructure.mapper.AutoServiceJpaMapper;
import br.com.fiap.postech.carworkshop.autoservice.infrastructure.persistence.AutoServiceJpaEntity;
import br.com.fiap.postech.carworkshop.autoservice.infrastructure.persistence.AutoServicePanacheRepository;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AutoServiceRepositoryGateway implements AutoServiceRepositoryPort {

    @Inject
    AutoServicePanacheRepository repository;

    @Inject
    AutoServiceJpaMapper mapper;

    @Override
    public List<AutoService> findAll() {
        return repository.findAll().list().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AutoService> findById(Long id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public List<AutoService> findByIds(List<Long> ids) {
        return repository.findByIds(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public AutoService save(AutoService autoService) {
        if (autoService.getId() != null) {
            AutoServiceJpaEntity existing = repository.findById(autoService.getId());
            if (existing != null) {
                existing.setDescription(autoService.getDescription());
                existing.setPrice(autoService.getPrice());
                return mapper.toDomain(existing);
            }
        }
        AutoServiceJpaEntity entity = mapper.toJpaEntity(autoService);
        entity.id = null;
        repository.persistAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return repository.deleteById(id);
    }
}
