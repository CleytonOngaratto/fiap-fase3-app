package br.com.fiap.postech.carworkshop.vehicle.adapter.presenter;

import br.com.fiap.postech.carworkshop.vehicle.domain.entity.Vehicle;

public record VehicleResponse(Long id, String vehiclePlate, String manufacturer, String modelName, Integer modelYear,
                               Long customerId) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getVehiclePlate(), vehicle.getManufacturer(),
                vehicle.getModelName(), vehicle.getModelYear(), vehicle.getCustomerId());
    }
}
