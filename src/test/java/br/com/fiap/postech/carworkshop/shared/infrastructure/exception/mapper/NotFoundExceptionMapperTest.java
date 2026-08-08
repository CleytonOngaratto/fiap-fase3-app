package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.NotFoundExceptionMapper;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotFoundExceptionMapperTest {

    private NotFoundExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new NotFoundExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = NotFoundExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn404() {
        Response response = mapper.toResponse(new NotFoundException("resource not found"));
        assertEquals(404, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_entityShouldContainMessage() {
        Response response = mapper.toResponse(new NotFoundException("resource not found"));
        StandardError error = (StandardError) response.getEntity();
        assertEquals("resource not found", error.getError());
    }
}
