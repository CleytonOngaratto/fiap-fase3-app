package br.com.fiap.postech.carworkshop.vehicle.usecase.interactor;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;
import br.com.fiap.postech.carworkshop.vehicle.domain.entity.Vehicle;
import br.com.fiap.postech.carworkshop.vehicle.usecase.interactor.VehicleInteractor;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleInteractorTest {

    @InjectMocks
    VehicleInteractor interactor;

    @Mock
    VehicleRepositoryPort repository;

    @Mock
    CustomerExistencePort customerDataPort;

    private Vehicle vehicleDomain;
    private VehicleRequest vehicleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vehicleDomain = Vehicle.builder().id(1L).vehiclePlate("ABC-1234").manufacturer("Toyota")
                .modelName("Corolla").modelYear(2020).customerId(1L).build();
        vehicleRequest = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, 1L);
    }

    @Test
    void findAll_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(vehicleDomain));
        List<VehicleResponse> result = interactor.findAll();
        assertEquals(1, result.size());
        assertEquals("ABC-1234", result.get(0).vehiclePlate());
    }

    @Test
    void findById_shouldReturnVehicle() {
        when(repository.findById(1L)).thenReturn(Optional.of(vehicleDomain));
        VehicleResponse result = interactor.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void findById_notFound_throwsEntityNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.findById(1L));
    }

    @Test
    void findById_invalidId_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.findById(0L));
        verifyNoInteractions(repository);
    }

    @Test
    void findByVehiclePlate_shouldReturnVehicle() {
        when(repository.findByVehiclePlate("ABC-1234")).thenReturn(Optional.of(vehicleDomain));
        VehicleResponse result = interactor.findByVehiclePlate("ABC-1234");
        assertNotNull(result);
    }

    @Test
    void create_shouldPersistAndReturn() {
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());
        when(customerDataPort.existsById(1L)).thenReturn(true);
        when(repository.save(any())).thenReturn(vehicleDomain);

        VehicleResponse result = interactor.create(vehicleRequest);
        assertNotNull(result);
        verify(repository).save(any(Vehicle.class));
    }

    @Test
    void create_duplicatePlate_throwsValidation() {
        when(repository.findByVehiclePlate("ABC-1234")).thenReturn(Optional.of(vehicleDomain));
        assertThrows(ValidationException.class, () -> interactor.create(vehicleRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void create_customerNotFound_throwsEntityNotFound() {
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());
        when(customerDataPort.existsById(1L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> interactor.create(vehicleRequest));
    }

    @Test
    void create_nullCustomerId_throwsValidation_andDoesNotPersist() {
        VehicleRequest noOwner = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, null);
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> interactor.create(noOwner));

        verify(customerDataPort, never()).existsById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void update_shouldUpdateSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(vehicleDomain));
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(vehicleDomain);
        assertDoesNotThrow(() -> interactor.update(1L, vehicleRequest));
    }

    @Test
    void update_doesNotChangeOwner_whenRequestCarriesDifferentCustomerId() {
        // D2 regression: owner is immutable on update — a different customerId in the request is ignored.
        when(repository.findById(1L)).thenReturn(Optional.of(vehicleDomain)); // existing owner = 1L
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(vehicleDomain);
        VehicleRequest withDifferentOwner = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, 2L);

        interactor.update(1L, withDifferentOwner);

        ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
        verify(repository).save(saved.capture());
        assertEquals(1L, saved.getValue().getCustomerId(), "owner must not change on update");
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.update(1L, vehicleRequest));
    }

    @Test
    void delete_shouldDeleteSuccessfully() {
        when(repository.deleteById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> interactor.delete(1L));
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        when(repository.deleteById(1L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> interactor.delete(1L));
    }
}
