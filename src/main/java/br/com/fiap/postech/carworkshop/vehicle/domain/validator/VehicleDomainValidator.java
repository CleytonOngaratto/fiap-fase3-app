package br.com.fiap.postech.carworkshop.vehicle.domain.validator;

import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;

public class VehicleDomainValidator {

    private static final String OLD_PLATE_REGEX = "^[A-Z]{3}-\\d{4}$";
    private static final String MERCOSUL_PLATE_REGEX = "^[A-Z]{3}[0-9][A-Z][0-9]{2}$";

    public void validate(VehicleRequest request) {
        if (request.vehiclePlate() == null || request.vehiclePlate().isBlank()) {
            throw new ValidationException("Vehicle plate must not be blank.");
        }
        if (request.manufacturer() == null || request.manufacturer().isBlank()) {
            throw new ValidationException("Vehicle manufacturer must not be blank.");
        }
        if (request.modelName() == null || request.modelName().isBlank()) {
            throw new ValidationException("Vehicle model name must not be blank.");
        }
        if (request.modelYear() == null || request.modelYear() <= 0) {
            throw new ValidationException("Vehicle model year must be greater than zero.");
        }
        validatePlate(request.vehiclePlate());
    }

    public void validatePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            throw new ValidationException("Vehicle plate must not be blank.");
        }
        if (!plate.matches(MERCOSUL_PLATE_REGEX) && !plate.matches(OLD_PLATE_REGEX)) {
            throw new ValidationException("Vehicle plate format is invalid.");
        }
    }
}
