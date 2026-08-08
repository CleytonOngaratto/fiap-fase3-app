package br.com.fiap.postech.carworkshop.auth.infrastructure.adapter;

import br.com.fiap.postech.carworkshop.auth.infrastructure.adapter.SecurityAdapter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StringHelpersTest {

    @Inject
    SecurityAdapter securityAdapter;

    @Test
    void generateSalt_shouldReturnValidBase64() {
        String salt = securityAdapter.generateSalt();
        assertNotNull(salt);
        assertFalse(salt.isEmpty());
        assertDoesNotThrow(() -> Base64.getDecoder().decode(salt));
    }

    @Test
    void generateSalt_decodedShouldBe16Bytes() {
        byte[] decoded = Base64.getDecoder().decode(securityAdapter.generateSalt());
        assertEquals(16, decoded.length);
    }

    @Test
    void generateSalt_shouldProduceDifferentValues() {
        String s1 = securityAdapter.generateSalt();
        String s2 = securityAdapter.generateSalt();
        assertNotEquals(s1, s2);
    }

    @Test
    void generateSalt_shouldOnlyContainBase64Chars() {
        String salt = securityAdapter.generateSalt();
        assertTrue(salt.matches("^[A-Za-z0-9+/]*={0,2}$"));
    }
}
