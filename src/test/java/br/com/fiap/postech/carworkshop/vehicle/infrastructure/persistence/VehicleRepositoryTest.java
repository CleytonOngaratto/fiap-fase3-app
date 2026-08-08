package br.com.fiap.postech.carworkshop.vehicle.infrastructure.persistence;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VehicleRepositoryTest {

    @Inject
    VehiclePanacheRepository vehicleRepository;

    // Isolamento: @TestTransaction reverte tudo ao final, então o deleteAll() (clean slate — este
    // teste usa a placa ABC-1234 do seed e checa listAll().size()) roda dentro da transação e é
    // desfeito no rollback, preservando o seed compartilhado para as outras classes.

    @Test
    @TestTransaction
    void testFindByVehiclePlate_should_return_vehicle_when_plate_exists() {
        vehicleRepository.deleteAll();
        var vehicle = VehicleJpaEntity.builder()
                .vehiclePlate("ABC-1234").manufacturer("Toyota")
                .modelName("Corolla").modelYear(2020).build();
        vehicleRepository.persist(vehicle);

        var result = vehicleRepository.findByVehiclePlate("ABC-1234");

        assertTrue(result.isPresent());
        assertEquals("ABC-1234", result.get().getVehiclePlate());
        assertEquals("Toyota", result.get().getManufacturer());
    }

    @Test
    @TestTransaction
    void testFindByVehiclePlate_should_return_empty_when_plate_does_not_exist() {
        vehicleRepository.deleteAll();
        assertTrue(vehicleRepository.findByVehiclePlate("XYZ-9999").isEmpty());
    }

    @Test
    @TestTransaction
    void testPersistAndFindById_should_work_correctly() {
        vehicleRepository.deleteAll();
        var vehicle = VehicleJpaEntity.builder()
                .vehiclePlate("DEF-5678").manufacturer("Honda")
                .modelName("Civic").modelYear(2021).build();
        vehicleRepository.persist(vehicle);

        var result = vehicleRepository.findByIdOptional(vehicle.id);

        assertTrue(result.isPresent());
        assertEquals("DEF-5678", result.get().getVehiclePlate());
    }

    @Test
    @TestTransaction
    void testListAll_should_return_all_vehicles() {
        vehicleRepository.deleteAll();
        vehicleRepository.persist(VehicleJpaEntity.builder().vehiclePlate("GHI-1111")
                .manufacturer("Ford").modelName("Focus").modelYear(2019).build());
        vehicleRepository.persist(VehicleJpaEntity.builder().vehiclePlate("JKL-2222")
                .manufacturer("Chevrolet").modelName("Onix").modelYear(2022).build());

        assertEquals(2, vehicleRepository.listAll().size());
    }
}
