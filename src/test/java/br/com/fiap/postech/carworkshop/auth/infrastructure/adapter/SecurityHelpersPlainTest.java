package br.com.fiap.postech.carworkshop.auth.infrastructure.adapter;

import br.com.fiap.postech.carworkshop.auth.infrastructure.adapter.SecurityAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SecurityHelpersPlainTest {

    private SecurityAdapter securityAdapter;

    @BeforeEach
    void setUp() {
        securityAdapter = new SecurityAdapter();
    }

    @Test
    void generateSalt_shouldReturnNonNullBase64String() {
        String salt = securityAdapter.generateSalt();
        assertNotNull(salt);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(salt));
    }

    @Test
    void generateSalt_shouldProduceDifferentValuesEachTime() {
        String salt1 = securityAdapter.generateSalt();
        String salt2 = securityAdapter.generateSalt();
        assertNotEquals(salt1, salt2);
    }

    @Test
    void generatePassword_shouldReturn64CharHexString() {
        String salt = securityAdapter.generateSalt();
        String hash = securityAdapter.hashPassword("user", "pass", salt);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[a-f0-9]{64}$"));
    }

    @Test
    void generatePassword_shouldBeDeterministicForSameInputs() {
        String salt = securityAdapter.generateSalt();
        String hash1 = securityAdapter.hashPassword("user", "pass", salt);
        String hash2 = securityAdapter.hashPassword("user", "pass", salt);
        assertEquals(hash1, hash2);
    }

    @Test
    void generatePassword_shouldDifferForDifferentSalts() {
        String hash1 = securityAdapter.hashPassword("user", "pass", "salt1");
        String hash2 = securityAdapter.hashPassword("user", "pass", "salt2");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void generatePassword_shouldDifferForDifferentPasswords() {
        String salt = securityAdapter.generateSalt();
        String hash1 = securityAdapter.hashPassword("user", "pass1", salt);
        String hash2 = securityAdapter.hashPassword("user", "pass2", salt);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateSalt_decodedLengthShouldBe16() {
        byte[] decoded = Base64.getDecoder().decode(securityAdapter.generateSalt());
        assertEquals(16, decoded.length);
    }

    @Test
    void generatePassword_shouldDifferForDifferentUsernames() {
        String salt = securityAdapter.generateSalt();
        String hash1 = securityAdapter.hashPassword("user1", "pass", salt);
        String hash2 = securityAdapter.hashPassword("user2", "pass", salt);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void generatePassword_shouldHandleEmptyUsername() {
        String hash = securityAdapter.hashPassword("", "pass", securityAdapter.generateSalt());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[a-f0-9]{64}$"));
    }

    @ParameterizedTest
    @MethodSource("provideCredentials")
    void generatePassword_shouldAlwaysProduce64CharHex(String username, String password) {
        String hash = securityAdapter.hashPassword(username, password, securityAdapter.generateSalt());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[a-f0-9]{64}$"));
    }

    private static Stream<Arguments> provideCredentials() {
        return Stream.of(
                Arguments.of("admin", "admin123"),
                Arguments.of("customer", "customer456"),
                Arguments.of("a", "b"),
                Arguments.of("verylongusername", "verylongpassword")
        );
    }
}
