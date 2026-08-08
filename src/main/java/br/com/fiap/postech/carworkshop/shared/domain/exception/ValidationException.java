package br.com.fiap.postech.carworkshop.shared.domain.exception;

public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
