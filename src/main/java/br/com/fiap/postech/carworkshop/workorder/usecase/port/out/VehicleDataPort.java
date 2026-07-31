package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import java.util.Optional;

public interface VehicleDataPort {
    Optional<VehicleInfo> findById(Long id);

    record VehicleInfo(Long id, String vehiclePlate) {}
}
