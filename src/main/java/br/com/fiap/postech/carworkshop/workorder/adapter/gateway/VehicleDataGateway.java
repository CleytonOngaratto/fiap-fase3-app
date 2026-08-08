package br.com.fiap.postech.carworkshop.workorder.adapter.gateway;

import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.VehicleDataPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class VehicleDataGateway implements VehicleDataPort {

    @Inject
    VehicleRepositoryPort vehicleRepository;

    @Override
    public Optional<VehicleInfo> findById(Long id) {
        return vehicleRepository.findById(id)
                .map(vehicle -> new VehicleInfo(vehicle.getId(), vehicle.getVehiclePlate()));
    }
}
