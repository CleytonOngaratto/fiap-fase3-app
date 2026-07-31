package br.com.fiap.postech.carworkshop.vehicle.usecase.port.in;

import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;

import java.util.List;

public interface VehicleUseCase {
    List<VehicleResponse> findAll();
    VehicleResponse findById(Long id);
    VehicleResponse findByVehiclePlate(String vehiclePlate);
    VehicleResponse create(VehicleRequest request);
    void update(Long id, VehicleRequest request);
    void delete(Long id);
}
