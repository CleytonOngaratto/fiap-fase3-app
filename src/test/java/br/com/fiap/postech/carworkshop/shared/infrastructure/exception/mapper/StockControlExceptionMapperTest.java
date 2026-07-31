package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.domain.exception.StockException;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper.StockExceptionMapper;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockControlExceptionMapperTest {

    private StockExceptionMapper mapper;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new StockExceptionMapper();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        Field field = StockExceptionMapper.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        field.set(mapper, uriInfo);
    }

    @Test
    void toResponse_shouldReturn422() {
        Response response = mapper.toResponse(new StockException("out of stock"));
        assertEquals(422, response.getStatus());
        assertInstanceOf(StandardError.class, response.getEntity());
    }

    @Test
    void toResponse_entityShouldContainMessage() {
        Response response = mapper.toResponse(new StockException("insufficient quantity"));
        StandardError error = (StandardError) response.getEntity();
        assertEquals("insufficient quantity", error.getError());
        assertEquals(422, error.getStatus());
    }
}
