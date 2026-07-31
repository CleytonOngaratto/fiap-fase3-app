package br.com.fiap.postech.carworkshop.inventory.adapter.gateway;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.infrastructure.mapper.PartsAndSupplyJpaMapper;
import br.com.fiap.postech.carworkshop.inventory.infrastructure.persistence.PartsAndSupplyJpaEntity;
import br.com.fiap.postech.carworkshop.inventory.infrastructure.persistence.PartsAndSupplyPanacheRepository;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.out.InventoryRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InventoryRepositoryGateway implements InventoryRepositoryPort {

    @Inject
    PartsAndSupplyPanacheRepository repository;

    @Inject
    PartsAndSupplyJpaMapper mapper;

    @Override
    public List<PartsAndSupply> findAll() {
        return repository.findAll().list().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PartsAndSupply> findById(Long id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public List<PartsAndSupply> findByIds(List<Long> ids) {
        return repository.findByIds(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public PartsAndSupply save(PartsAndSupply part) {
        if (part.getId() != null) {
            PartsAndSupplyJpaEntity existing = repository.findById(part.getId());
            existing.setCode(part.getCode());
            existing.setManufacturer(part.getManufacturer());
            existing.setDescription(part.getDescription());
            existing.setPrice(part.getPrice());
            existing.setType(part.getType());
            existing.setQuantity(part.getQuantity());
            return mapper.toDomain(existing);
        }
        PartsAndSupplyJpaEntity entity = mapper.toJpaEntity(part);
        repository.persistAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return repository.deleteById(id);
    }
}
