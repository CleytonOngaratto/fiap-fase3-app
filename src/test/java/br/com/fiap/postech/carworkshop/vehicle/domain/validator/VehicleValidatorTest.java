package br.com.fiap.postech.carworkshop.vehicle.domain.validator;

import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.domain.validator.VehicleDomainValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VehicleValidatorTest {

    private VehicleDomainValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VehicleDomainValidator();
    }

    private VehicleRequest validRequest() {
        return new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2023, 1L);
    }

    @Test
    void testValidate_Success() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidVehiclePlate(String invalidPlate) {
        var request = new VehicleRequest(invalidPlate, "Toyota", "Corolla", 2023, 1L);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Vehicle plate must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidManufacturer(String invalidManufacturer) {
        var request = new VehicleRequest("ABC-1234", invalidManufacturer, "Corolla", 2023, 1L);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Vehicle manufacturer must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidModelName(String invalidModelName) {
        var request = new VehicleRequest("ABC-1234", "Toyota", invalidModelName, 2023, 1L);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Vehicle model name must not be blank.", ex.getMessage());
    }

    @Test
    void testValidate_NullModelYear() {
        var request = new VehicleRequest("ABC-1234", "Toyota", "Corolla", null, 1L);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Vehicle model year must be greater than zero.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void testValidate_InvalidModelYear(Integer invalidYear) {
        var request = new VehicleRequest("ABC-1234", "Toyota", "Corolla", invalidYear, 1L);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Vehicle model year must be greater than zero.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC-1234", "XYZ-9876", "ABC1D23", "XYZ9A99", "DEF9J34"})
    void testValidatePlate_ValidFormats(String validPlate) {
        assertDoesNotThrow(() -> validator.validatePlate(validPlate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "ABC12345", "ABCD123", "123ABCD", "abc1234", "ABC 1234"})
    void testValidatePlate_InvalidFormats(String invalidPlate) {
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validatePlate(invalidPlate));
        assertEquals("Vehicle plate format is invalid.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void testValidatePlate_NullAndBlank(String blankPlate) {
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validatePlate(blankPlate));
        assertEquals("Vehicle plate must not be blank.", ex.getMessage());
    }
}
