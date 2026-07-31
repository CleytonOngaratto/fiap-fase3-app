package br.com.fiap.postech.carworkshop.autoservice.infrastructure.config;

import br.com.fiap.postech.carworkshop.autoservice.usecase.interactor.AutoServiceInteractor;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.in.AutoServiceUseCase;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the auto service use case. The framework annotation lives HERE so the
 * {@link AutoServiceInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Mirrors the AuthModuleConfig reference pattern.
 */
@ApplicationScoped
public class AutoServiceModuleConfig {

    @Produces
    @ApplicationScoped
    public AutoServiceUseCase autoServiceUseCase(AutoServiceRepositoryPort repository) {
        return new AutoServiceInteractor(repository);
    }
}
