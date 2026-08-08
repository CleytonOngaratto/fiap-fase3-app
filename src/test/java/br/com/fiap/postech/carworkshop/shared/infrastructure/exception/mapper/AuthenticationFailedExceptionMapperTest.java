package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.AuthenticationFailedExceptionMapper;
import io.quarkus.security.AuthenticationFailedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFailedExceptionMapperTest {

    private AuthenticationFailedExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new AuthenticationFailedExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = AuthenticationFailedExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn401() {
        Response response = mapper.toResponse(new AuthenticationFailedException("token expired"));
        assertEquals(401, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_shouldUseFixedErrorMessage() {
        Response response = mapper.toResponse(new AuthenticationFailedException("anything"));
        StandardError error = (StandardError) response.getEntity();
        assertEquals("Invalid or expired token", error.getError());
    }
}
