package br.com.fiap.postech.carworkshop.customer.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CustomerPanacheRepository implements PanacheRepository<CustomerJpaEntity> {

    public Optional<CustomerJpaEntity> findByDocument(String document) {
        return find("document", document).firstResultOptional();
    }

    public Optional<CustomerJpaEntity> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
