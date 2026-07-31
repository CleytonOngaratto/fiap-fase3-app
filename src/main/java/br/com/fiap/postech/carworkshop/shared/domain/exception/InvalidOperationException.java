package br.com.fiap.postech.carworkshop.shared.domain.exception;

public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
