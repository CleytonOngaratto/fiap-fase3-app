package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.UnauthorizedExceptionMapper;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnauthorizedExceptionMapperTest {

    private UnauthorizedExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new UnauthorizedExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = UnauthorizedExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn401() {
        Response response = mapper.toResponse(new UnauthorizedException("not authorized"));
        assertEquals(401, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_shouldUseDefaultMessageWhenNull() {
        Response response = mapper.toResponse(new UnauthorizedException((String) null));
        StandardError error = (StandardError) response.getEntity();
        assertEquals("Unauthorized", error.getError());
    }
}
