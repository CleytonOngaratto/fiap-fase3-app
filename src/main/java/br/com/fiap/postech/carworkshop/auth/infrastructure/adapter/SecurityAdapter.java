package br.com.fiap.postech.carworkshop.auth.infrastructure.adapter;

import br.com.fiap.postech.carworkshop.auth.usecase.port.out.SecurityPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class SecurityAdapter implements SecurityPort {

    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";
    private final String secretKey = System.getenv("SECRET_KEY");

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public String hashPassword(String username, String rawPassword, String salt) {
        String key = salt + username + rawPassword + secretKey;
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(key.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerErrorException("Error creating user");
        }
    }
}
