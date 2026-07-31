package br.com.fiap.postech.carworkshop.fixtures;

import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;
import br.com.fiap.postech.carworkshop.vehicle.infrastructure.persistence.VehicleJpaEntity;

public class VehicleFixtures {
    public static final String VEHICLE_PLATE = "ABC-1234";
    public static final String MANUFACTURER = "Toyota";
    public static final String MODEL_NAME = "Corolla";
    public static final Integer MODEL_YEAR = 2020;

    public static VehicleResponse getVehicleDTO() {
        return new VehicleResponse(1L, VEHICLE_PLATE, MANUFACTURER, MODEL_NAME, MODEL_YEAR, 1L);
    }

    public static VehicleRequest getVehicleRequest() {
        return new VehicleRequest(VEHICLE_PLATE, MANUFACTURER, MODEL_NAME, MODEL_YEAR, 1L);
    }

    public static VehicleJpaEntity getVehicleEntity() {
        VehicleJpaEntity entity = VehicleJpaEntity.builder()
                .vehiclePlate(VEHICLE_PLATE).manufacturer(MANUFACTURER)
                .modelName(MODEL_NAME).modelYear(MODEL_YEAR).customerId(1L).build();
        entity.id = 1L;
        return entity;
    }
}
