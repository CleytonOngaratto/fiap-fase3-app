package br.com.fiap.postech.carworkshop.vehicle.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class VehiclePanacheRepository implements PanacheRepository<VehicleJpaEntity> {

    public Optional<VehicleJpaEntity> findByVehiclePlate(String vehiclePlate) {
        return find("vehiclePlate", vehiclePlate).firstResultOptional();
    }
}
