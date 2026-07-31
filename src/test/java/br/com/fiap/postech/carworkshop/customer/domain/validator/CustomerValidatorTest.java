package br.com.fiap.postech.carworkshop.customer.domain.validator;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.domain.validator.CustomerDomainValidator;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerValidatorTest {

    private CustomerDomainValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CustomerDomainValidator();
    }

    private CustomerRequest validRequest() {
        return new CustomerRequest("João Silva", "12345678909", "MG1234567", "joao@email.com", "11999999999");
    }

    @Test
    void testValidate_Success() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidName(String invalidName) {
        var request = new CustomerRequest(invalidName, "12345678909", "MG1234567", "joao@email.com", "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Customer name must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidDocument(String invalidDocument) {
        var request = new CustomerRequest("João", invalidDocument, "MG1234567", "joao@email.com", "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Customer CPF/CNPJ must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidRg(String invalidRg) {
        var request = new CustomerRequest("João", "12345678909", invalidRg, "joao@email.com", "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Customer RG must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidEmail(String invalidEmail) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", invalidEmail, "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Customer email must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testValidate_InvalidPhoneNumber(String invalidNumber) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", "joao@email.com", invalidNumber);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Customer phone number must not be blank.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678909", "11144477735", "52998224725"})
    void testValidate_ValidCpfs(String cpf) {
        var request = new CustomerRequest("João", cpf, "MG1234567", "joao@email.com", "11999999999");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "123456789012", "12345678901"})
    void testValidate_InvalidCpf(String invalidCpf) {
        var request = new CustomerRequest("João", invalidCpf, "MG1234567", "joao@email.com", "11999999999");
        assertThrows(ValidationException.class, () -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222"})
    void testValidate_RepeatedDigitCpf(String cpf) {
        var request = new CustomerRequest("João", cpf, "MG1234567", "joao@email.com", "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("CPF is invalid.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name@example.com", "user+tag@example.co.uk"})
    void testValidate_ValidEmails(String email) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", email, "11999999999");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "test@", "@example.com", "test@@example.com"})
    void testValidate_InvalidEmails(String email) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", email, "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Email format is invalid.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"11999999999", "11988888888", "21987654321"})
    void testValidate_ValidPhones(String phone) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", "joao@email.com", phone);
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234567890123", "(11) 99999-9999"})
    void testValidate_InvalidPhones(String phone) {
        var request = new CustomerRequest("João", "12345678909", "MG1234567", "joao@email.com", phone);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Phone number format is invalid.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "45543915000181"})
    void testValidate_ValidCnpjs(String cnpj) {
        var request = new CustomerRequest("Empresa Ltda", cnpj, "IE12345", "empresa@domain.com.br", "11987654321");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "11111111111111"})
    void testValidate_RepeatedDigitCnpj(String cnpj) {
        var request = new CustomerRequest("Empresa", cnpj, "IE12345", "empresa@domain.com.br", "11987654321");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("CNPJ is invalid.", ex.getMessage());
    }

    @Test
    void testValidate_InvalidDocumentLength() {
        var request = new CustomerRequest("João", "123456789", "MG1234567", "joao@email.com", "11999999999");
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertEquals("Document must be a valid CPF (11 digits) or CNPJ (14 digits).", ex.getMessage());
    }
}
