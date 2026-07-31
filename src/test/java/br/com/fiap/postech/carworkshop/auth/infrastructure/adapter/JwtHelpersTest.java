package br.com.fiap.postech.carworkshop.auth.infrastructure.adapter;

import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;
import br.com.fiap.postech.carworkshop.auth.infrastructure.adapter.JwtTokenAdapter;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class JwtHelpersTest {

    @Inject
    JwtTokenAdapter jwtTokenAdapter;

    @Inject
    JWTParser jwtParser;

    @Test
    void testGenerateToken_WithSingleRole() throws ParseException {
        String token = jwtTokenAdapter.generateToken("testuser", List.of(UserRoles.USER));
        assertNotNull(token);
        assertFalse(token.isEmpty());
        JsonWebToken jwt = jwtParser.parse(token);
        assertEquals("testuser", jwt.getSubject());
        assertTrue(jwt.getGroups().contains("USER"));
    }

    @Test
    void testGenerateToken_WithMultipleRoles() throws ParseException {
        String token = jwtTokenAdapter.generateToken("admin", Arrays.asList(UserRoles.ADMIN, UserRoles.USER));
        JsonWebToken jwt = jwtParser.parse(token);
        assertTrue(jwt.getGroups().contains("ADMIN"));
        assertTrue(jwt.getGroups().contains("USER"));
    }

    @Test
    void testGenerateToken_WithEmptyRoles() throws ParseException {
        String token = jwtTokenAdapter.generateToken("noroleuser", Collections.emptyList());
        JsonWebToken jwt = jwtParser.parse(token);
        assertEquals("noroleuser", jwt.getSubject());
        assertTrue(jwt.getGroups().isEmpty());
    }

    @Test
    void testGenerateToken_TokenHasIssuer() throws ParseException {
        String token = jwtTokenAdapter.generateToken("testuser", List.of(UserRoles.USER));
        JsonWebToken jwt = jwtParser.parse(token);
        assertNotNull(jwt.getIssuer());
        assertFalse(jwt.getIssuer().isEmpty());
    }

    @Test
    void testGenerateToken_TokenHasExpiration() throws ParseException {
        String token = jwtTokenAdapter.generateToken("testuser", List.of(UserRoles.USER));
        JsonWebToken jwt = jwtParser.parse(token);
        assertNotNull(jwt.getExpirationTime());
        assertTrue(jwt.getExpirationTime() > System.currentTimeMillis() / 1000);
    }

    @Test
    void testGenerateToken_DifferentTokensForDifferentUsers() {
        String token1 = jwtTokenAdapter.generateToken("user1", List.of(UserRoles.USER));
        String token2 = jwtTokenAdapter.generateToken("user2", List.of(UserRoles.USER));
        assertNotEquals(token1, token2);
    }

    @ParameterizedTest
    @MethodSource("provideUsernamesAndRoles")
    void testGenerateToken_WithVariousUsernamesAndRoles(String username, List<UserRoles> roles) throws ParseException {
        String token = jwtTokenAdapter.generateToken(username, roles);
        assertNotNull(token);
        JsonWebToken jwt = jwtParser.parse(token);
        assertEquals(username, jwt.getSubject());
        assertEquals(roles.size(), jwt.getGroups().size());
        for (UserRoles role : roles) {
            assertTrue(jwt.getGroups().contains(role.name()));
        }
    }

    private static Stream<Arguments> provideUsernamesAndRoles() {
        return Stream.of(
                Arguments.of("user1", List.of(UserRoles.USER)),
                Arguments.of("admin1", List.of(UserRoles.ADMIN)),
                Arguments.of("customer1", List.of(UserRoles.CUSTOMER)),
                Arguments.of("multiuser", Arrays.asList(UserRoles.USER, UserRoles.CUSTOMER)),
                Arguments.of("noroles", Collections.emptyList())
        );
    }
}
