package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.BadRequestExceptionMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BadRequestExceptionMapperTest {

    private BadRequestExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new BadRequestExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = BadRequestExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn400() {
        Response response = mapper.toResponse(new BadRequestException("bad input"));
        assertEquals(400, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_entityShouldContainMessage() {
        Response response = mapper.toResponse(new BadRequestException("invalid field"));
        StandardError error = (StandardError) response.getEntity();
        assertEquals("invalid field", error.getError());
    }
}
