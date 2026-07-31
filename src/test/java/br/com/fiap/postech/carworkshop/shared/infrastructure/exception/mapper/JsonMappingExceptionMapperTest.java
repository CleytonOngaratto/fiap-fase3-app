package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.JsonMappingExceptionMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonMappingExceptionMapperTest {

    private JsonMappingExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new JsonMappingExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = JsonMappingExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn400() {
        JsonMappingException ex = mock(JsonMappingException.class);
        when(ex.getMessage()).thenReturn("Cannot deserialize value");
        when(ex.getOriginalMessage()).thenReturn("Cannot deserialize value");

        Response response = mapper.toResponse(ex);
        assertEquals(400, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_entityShouldContainMessage() {
        JsonMappingException ex = mock(JsonMappingException.class);
        when(ex.getMessage()).thenReturn("bad json");
        when(ex.getOriginalMessage()).thenReturn("bad json");

        Response response = mapper.toResponse(ex);
        StandardError error = (StandardError) response.getEntity();
        assertEquals("bad json", error.getError());
    }
}
