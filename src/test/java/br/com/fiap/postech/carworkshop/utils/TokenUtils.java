package br.com.fiap.postech.carworkshop.utils;

import io.smallrye.jwt.build.Jwt;

import java.util.Arrays;
import java.util.HashSet;

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
}

