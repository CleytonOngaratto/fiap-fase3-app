package br.com.fiap.postech.carworkshop.customer.adapter.gateway;

import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import br.com.fiap.postech.carworkshop.customer.infrastructure.mapper.CustomerJpaMapper;
import br.com.fiap.postech.carworkshop.customer.infrastructure.persistence.CustomerJpaEntity;
import br.com.fiap.postech.carworkshop.customer.infrastructure.persistence.CustomerPanacheRepository;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CustomerRepositoryGateway implements CustomerRepositoryPort {

    @Inject
    CustomerPanacheRepository repository;

    @Inject
    CustomerJpaMapper mapper;

    @Override
    public List<Customer> findAll() {
        return repository.findAll().list().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByDocument(String document) {
        return repository.findByDocument(document).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        if (customer.getId() != null) {
            CustomerJpaEntity existing = repository.findById(customer.getId());
            existing.setName(customer.getName());
            existing.setDocument(customer.getDocument());
            existing.setRg(customer.getRg());
            existing.setEmail(customer.getEmail());
            existing.setNumber(customer.getNumber());
            return mapper.toDomain(existing);
        }
        CustomerJpaEntity entity = mapper.toJpaEntity(customer);
        repository.persistAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return repository.deleteById(id);
    }
}
