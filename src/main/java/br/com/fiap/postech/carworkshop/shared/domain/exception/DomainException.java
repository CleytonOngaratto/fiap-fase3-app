package br.com.fiap.postech.carworkshop.shared.domain.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
