package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.JsonProcessingExceptionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonProcessingExceptionMapperTest {

    private JsonProcessingExceptionMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new JsonProcessingExceptionMapper();
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = JsonProcessingExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn400() {
        JsonProcessingException ex = mock(JsonProcessingException.class);
        when(ex.getOriginalMessage()).thenReturn("Unexpected character (',')");

        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_entityShouldContainParseMessage() {
        JsonProcessingException ex = mock(JsonProcessingException.class);
        when(ex.getOriginalMessage()).thenReturn("malformed json");

        Response response = mapper.toResponse(ex);

        StandardError error = (StandardError) response.getEntity();
        assertEquals("malformed json", error.getError());
        assertEquals(400, error.getStatus());
    }
}
