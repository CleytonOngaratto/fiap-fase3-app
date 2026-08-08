package br.com.fiap.postech.carworkshop.shared.infrastructure.exception;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StandardErrorTest {

    @Test
    void of_shouldBuildWithCorrectFields() {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/test"));

        StandardError error = StandardError.of(
                Response.Status.BAD_REQUEST,
                new RuntimeException("validation failed"),
                uriInfo);

        assertEquals(400, error.getStatus());
        assertEquals("validation failed", error.getError());
        assertEquals("http://localhost/api/test", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void of_shouldSetTimestampCloseToNow() {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));

        Instant before = Instant.now();
        StandardError error = StandardError.of(
                Response.Status.NOT_FOUND,
                new RuntimeException("not found"),
                uriInfo);
        Instant after = Instant.now();

        assertNotNull(error.getTimestamp());
        assertFalse(error.getTimestamp().isBefore(before));
        assertFalse(error.getTimestamp().isAfter(after));
    }
}
