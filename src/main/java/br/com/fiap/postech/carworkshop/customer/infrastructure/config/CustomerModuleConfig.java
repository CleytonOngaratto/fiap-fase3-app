package br.com.fiap.postech.carworkshop.customer.infrastructure.config;

import br.com.fiap.postech.carworkshop.customer.usecase.interactor.CustomerInteractor;
import br.com.fiap.postech.carworkshop.customer.usecase.port.in.CustomerUseCase;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the customer use case. The framework annotation lives HERE so the
 * {@link CustomerInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Mirrors the AuthModuleConfig reference pattern.
 */
@ApplicationScoped
public class CustomerModuleConfig {

    @Produces
    @ApplicationScoped
    public CustomerUseCase customerUseCase(CustomerRepositoryPort repository) {
        return new CustomerInteractor(repository);
    }
}
