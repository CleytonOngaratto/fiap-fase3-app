package br.com.fiap.postech.carworkshop.auth.usecase.port.out;

public interface SecurityPort {
    String generateSalt();
    String hashPassword(String username, String rawPassword, String salt);
}
