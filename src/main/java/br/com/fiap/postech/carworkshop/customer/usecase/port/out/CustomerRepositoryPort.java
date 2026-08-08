package br.com.fiap.postech.carworkshop.customer.usecase.port.out;

import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {
    List<Customer> findAll();
    Optional<Customer> findById(Long id);
    Optional<Customer> findByDocument(String document);
    Optional<Customer> findByEmail(String email);
    Customer save(Customer customer);
    boolean deleteById(Long id);
}
