package br.com.fiap.postech.carworkshop.customer.usecase.interactor;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;
import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import br.com.fiap.postech.carworkshop.customer.domain.validator.CustomerDomainValidator;
import br.com.fiap.postech.carworkshop.customer.usecase.port.in.CustomerUseCase;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CustomerInteractor implements CustomerUseCase {

    private final CustomerRepositoryPort repository;

    private final CustomerDomainValidator validator = new CustomerDomainValidator();

    public CustomerInteractor(CustomerRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<CustomerResponse> findAll() {
        log.info("Finding all customers");
        return repository.findAll().stream().map(CustomerResponse::from).toList();
    }

    @Override
    public CustomerResponse findById(Long id) {
        log.info("Finding customer by id: {}", id);
        return repository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
    }

    @Override
    public CustomerResponse findByDocument(String document) {
        log.info("Finding customer by document: {}", document);
        if (document == null || document.isBlank()) {
            throw new ValidationException("Customer CPF must not be blank.");
        }
        return repository.findByDocument(document)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
    }

    @Override
    public CustomerResponse findByEmail(String email) {
        log.info("Finding customer by email: {}", email);
        if (email == null || email.isBlank()) {
            throw new ValidationException("Customer email must not be blank.");
        }
        return repository.findByEmail(email)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
    }

    @Override
    public CustomerResponse create(CustomerRequest request) {
        log.info("Creating customer: {}", request);
        validator.validate(request);
        repository.findByDocument(request.document())
                .ifPresent(c -> { throw new ValidationException("Customer with this CPF/CNPJ already exists."); });
        repository.findByEmail(request.email())
                .ifPresent(c -> { throw new ValidationException("Customer with this email already exists."); });

        Customer customer = Customer.builder()
                .name(request.name())
                .document(request.document())
                .rg(request.rg())
                .email(request.email())
                .number(request.number())
                .build();
        return CustomerResponse.from(repository.save(customer));
    }

    @Override
    public void update(Long id, CustomerRequest request) {
        log.info("Updating customer id: {}", id);
        if (id == null || id <= 0) {
            throw new ValidationException("Customer id must be a positive number.");
        }
        validator.validate(request);
        Customer existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
        repository.findByDocument(request.document())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new ValidationException("Customer with this CPF/CNPJ already exists."); });
        repository.findByEmail(request.email())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new ValidationException("Customer with this email already exists."); });
        existing.setName(request.name());
        existing.setDocument(request.document());
        existing.setRg(request.rg());
        existing.setEmail(request.email());
        existing.setNumber(request.number());
        repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting customer id: {}", id);
        if (id == null || id <= 0) {
            throw new ValidationException("Customer id must be a positive number.");
        }
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Customer not found.");
        }
    }
}
