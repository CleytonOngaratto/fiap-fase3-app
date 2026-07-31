package br.com.fiap.postech.carworkshop.inventory.infrastructure.config;

import br.com.fiap.postech.carworkshop.inventory.usecase.interactor.InventoryInteractor;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.in.InventoryUseCase;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.out.InventoryRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the inventory use case. The framework annotation lives HERE so the
 * {@link InventoryInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Mirrors the AuthModuleConfig reference pattern.
 */
@ApplicationScoped
public class InventoryModuleConfig {

    @Produces
    @ApplicationScoped
    public InventoryUseCase inventoryUseCase(InventoryRepositoryPort repository) {
        return new InventoryInteractor(repository);
    }
}
