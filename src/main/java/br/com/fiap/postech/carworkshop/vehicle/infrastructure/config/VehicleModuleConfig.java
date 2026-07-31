package br.com.fiap.postech.carworkshop.vehicle.infrastructure.config;

import br.com.fiap.postech.carworkshop.vehicle.usecase.interactor.VehicleInteractor;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.in.VehicleUseCase;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the vehicle use case. The framework annotation lives HERE so the
 * {@link VehicleInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Mirrors the AuthModuleConfig reference pattern.
 */
@ApplicationScoped
public class VehicleModuleConfig {

    @Produces
    @ApplicationScoped
    public VehicleUseCase vehicleUseCase(VehicleRepositoryPort repository,
                                         CustomerExistencePort customerExistencePort) {
        return new VehicleInteractor(repository, customerExistencePort);
    }
}
