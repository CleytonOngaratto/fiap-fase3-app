package br.com.fiap.postech.carworkshop.utils;

import io.smallrye.jwt.build.Jwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TokenUtils {

    public static String generateTokenString(String username, String... roles) {
        return Jwt.issuer("https://oficina-api.com")
                .upn(username)
                .groups(new HashSet<>(Arrays.asList(roles)))
                .sign();
    }

    public static String generateAdminToken() {
        return generateTokenString("testUser", "ADMIN");
    }

    public static String generateUserToken() {
        return generateTokenString("testUser", "USER");
    }

    /** Reproduz offline o token que a Lambda de autenticacao emite ao validar um CPF. */
    public static String generateCustomerToken(String cpf) {
        return Jwt.issuer("https://oficina-api.com")
                .subject(cpf)
                .upn(cpf)
                .groups(Set.of("CUSTOMER"))
                .claim("cpf", cpf)
                .sign();
    }

    public static String generateCustomerToken() {
        return generateCustomerToken("52998224725");
    }
}
