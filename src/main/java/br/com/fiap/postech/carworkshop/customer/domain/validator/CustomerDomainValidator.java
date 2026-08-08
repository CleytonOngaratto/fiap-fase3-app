package br.com.fiap.postech.carworkshop.customer.domain.validator;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;

public class CustomerDomainValidator {

    private static final String CPF_REGEX = "^\\d{11}$";
    private static final String CNPJ_REGEX = "^\\d{14}$";
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_REGEX = "^\\d{10,11}$";

    public void validate(CustomerRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Customer name must not be blank.");
        }
        if (request.document() == null || request.document().isBlank()) {
            throw new ValidationException("Customer CPF/CNPJ must not be blank.");
        }
        if (request.rg() == null || request.rg().isBlank()) {
            throw new ValidationException("Customer RG must not be blank.");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationException("Customer email must not be blank.");
        }
        if (request.number() == null || request.number().isBlank()) {
            throw new ValidationException("Customer phone number must not be blank.");
        }
        validateDocument(request.document());
        validateEmail(request.email());
        validatePhone(request.number());
    }

    private void validateDocument(String document) {
        if (document.length() == 11) {
            if (!document.matches(CPF_REGEX)) {
                throw new ValidationException("CPF format is invalid.");
            }
            if (!isValidCpf(document)) {
                throw new ValidationException("CPF is invalid.");
            }
        } else if (document.length() == 14) {
            if (!document.matches(CNPJ_REGEX)) {
                throw new ValidationException("CNPJ format is invalid.");
            }
            if (!isValidCnpj(document)) {
                throw new ValidationException("CNPJ is invalid.");
            }
        } else {
            throw new ValidationException("Document must be a valid CPF (11 digits) or CNPJ (14 digits).");
        }
    }

    private void validateEmail(String email) {
        if (!email.matches(EMAIL_REGEX)) {
            throw new ValidationException("Email format is invalid.");
        }
    }

    private void validatePhone(String number) {
        if (!number.matches(PHONE_REGEX)) {
            throw new ValidationException("Phone number format is invalid.");
        }
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;
        int[] digits = cpf.chars().map(c -> c - '0').toArray();
        int sum1 = 0;
        for (int i = 0; i < 9; i++) sum1 += digits[i] * (10 - i);
        int check1 = 11 - (sum1 % 11);
        if (check1 >= 10) check1 = 0;
        if (digits[9] != check1) return false;
        int sum2 = 0;
        for (int i = 0; i < 10; i++) sum2 += digits[i] * (11 - i);
        int check2 = 11 - (sum2 % 11);
        if (check2 >= 10) check2 = 0;
        return digits[10] == check2;
    }

    private boolean isValidCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        int[] digits = cnpj.chars().map(c -> c - '0').toArray();
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum1 = 0;
        for (int i = 0; i < 12; i++) sum1 += digits[i] * w1[i];
        int rem1 = sum1 % 11;
        int check1 = rem1 < 2 ? 0 : 11 - rem1;
        if (digits[12] != check1) return false;
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum2 = 0;
        for (int i = 0; i < 13; i++) sum2 += digits[i] * w2[i];
        int rem2 = sum2 % 11;
        int check2 = rem2 < 2 ? 0 : 11 - rem2;
        return digits[13] == check2;
    }
}
