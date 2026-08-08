package br.com.fiap.postech.carworkshop.inventory.infrastructure.config;

import br.com.fiap.postech.carworkshop.inventory.infrastructure.config.InventoryModuleConfig;
import br.com.fiap.postech.carworkshop.inventory.usecase.interactor.InventoryInteractor;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.in.InventoryUseCase;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.out.InventoryRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class InventoryModuleConfigTest {

    @Test
    void inventoryUseCase_buildsWiredInteractor() {
        InventoryUseCase useCase = new InventoryModuleConfig().inventoryUseCase(
                mock(InventoryRepositoryPort.class));

        assertNotNull(useCase);
        assertInstanceOf(InventoryInteractor.class, useCase);
    }
}
