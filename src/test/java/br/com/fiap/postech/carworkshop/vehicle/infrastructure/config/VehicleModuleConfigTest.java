package br.com.fiap.postech.carworkshop.vehicle.infrastructure.config;

import br.com.fiap.postech.carworkshop.vehicle.infrastructure.config.VehicleModuleConfig;
import br.com.fiap.postech.carworkshop.vehicle.usecase.interactor.VehicleInteractor;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.in.VehicleUseCase;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class VehicleModuleConfigTest {

    @Test
    void vehicleUseCase_buildsWiredInteractor() {
        VehicleUseCase useCase = new VehicleModuleConfig().vehicleUseCase(
                mock(VehicleRepositoryPort.class),
                mock(CustomerExistencePort.class));

        assertNotNull(useCase);
        assertInstanceOf(VehicleInteractor.class, useCase);
    }
}
