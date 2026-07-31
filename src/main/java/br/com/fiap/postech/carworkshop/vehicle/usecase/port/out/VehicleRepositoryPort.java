package br.com.fiap.postech.carworkshop.vehicle.usecase.port.out;

import br.com.fiap.postech.carworkshop.vehicle.domain.entity.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepositoryPort {
    List<Vehicle> findAll();
    Optional<Vehicle> findById(Long id);
    Optional<Vehicle> findByVehiclePlate(String vehiclePlate);
    Vehicle save(Vehicle vehicle);
    boolean deleteById(Long id);
}
