package br.com.fiap.postech.carworkshop.vehicle.usecase.interactor;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;
import br.com.fiap.postech.carworkshop.vehicle.domain.entity.Vehicle;
import br.com.fiap.postech.carworkshop.vehicle.domain.validator.VehicleDomainValidator;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.in.VehicleUseCase;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Framework-agnostic use case (D4/V4): no {@code jakarta.*}, dependencies injected by constructor.
 * CDI wiring lives in {@code vehicle.infrastructure.config.VehicleModuleConfig}.
 */
@Slf4j
public class VehicleInteractor implements VehicleUseCase {

    private final VehicleRepositoryPort repository;
    private final CustomerExistencePort customerExistencePort;

    private final VehicleDomainValidator validator = new VehicleDomainValidator();

    public VehicleInteractor(VehicleRepositoryPort repository, CustomerExistencePort customerExistencePort) {
        this.repository = repository;
        this.customerExistencePort = customerExistencePort;
    }

    @Override
    public List<VehicleResponse> findAll() {
        log.info("Finding all vehicles");
        return repository.findAll().stream().map(VehicleResponse::from).toList();
    }

    @Override
    public VehicleResponse findById(Long id) {
        log.info("Finding vehicle by id: {}", id);
        validateId(id);
        return repository.findById(id)
                .map(VehicleResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found."));
    }

    @Override
    public VehicleResponse findByVehiclePlate(String vehiclePlate) {
        log.info("Finding vehicle by plate: {}", vehiclePlate);
        if (vehiclePlate == null || vehiclePlate.isBlank()) {
            throw new ValidationException("Vehicle plate must not be blank.");
        }
        return repository.findByVehiclePlate(vehiclePlate)
                .map(VehicleResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found."));
    }

    @Override
    public VehicleResponse create(VehicleRequest request) {
        log.info("Creating vehicle: {}", request);
        validator.validate(request);
        repository.findByVehiclePlate(request.vehiclePlate())
                .ifPresent(v -> { throw new ValidationException("Vehicle plate already exists."); });
        // D2/D3: a vehicle ALWAYS has an owner — customerId is mandatory and must reference an
        // existing customer. Enforced here (no DB NOT NULL); the gateway only persists.
        if (request.customerId() == null || request.customerId() <= 0) {
            throw new ValidationException("customerId is required and must be greater than 0.");
        }
        if (!customerExistencePort.existsById(request.customerId())) {
            throw new EntityNotFoundException("Customer not found.");
        }
        Vehicle vehicle = Vehicle.builder()
                .vehiclePlate(request.vehiclePlate())
                .manufacturer(request.manufacturer())
                .modelName(request.modelName())
                .modelYear(request.modelYear())
                .customerId(request.customerId())
                .build();
        return VehicleResponse.from(repository.save(vehicle));
    }

    @Override
    public void update(Long id, VehicleRequest request) {
        log.info("Updating vehicle id: {}", id);
        validateId(id);
        validator.validate(request);
        Vehicle existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found."));
        repository.findByVehiclePlate(request.vehiclePlate())
                .filter(v -> !v.getId().equals(id))
                .ifPresent(v -> { throw new ValidationException("Vehicle plate already exists."); });
        existing.setVehiclePlate(request.vehiclePlate());
        existing.setManufacturer(request.manufacturer());
        existing.setModelName(request.modelName());
        existing.setModelYear(request.modelYear());
        // customerId is immutable — set only on create; the request's customerId is ignored here.
        repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting vehicle id: {}", id);
        validateId(id);
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Vehicle not found.");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid ID: must be a positive number.");
        }
    }
}
