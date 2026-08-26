package br.com.fiap.postech.carworkshop.shared.infrastructure.observability;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {

    private TraceIdFilter filter;
    private ContainerRequestContext request;
    private ContainerResponseContext response;
    private MultivaluedMap<String, Object> responseHeaders;
    private Map<String, Object> properties;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        request = Mockito.mock(ContainerRequestContext.class);
        response = Mockito.mock(ContainerResponseContext.class);
        responseHeaders = new MultivaluedHashMap<>();
        properties = new HashMap<>();

        when(response.getHeaders()).thenReturn(responseHeaders);
        Mockito.doAnswer(inv -> properties.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setProperty(anyString(), Mockito.any());
        when(request.getProperty(anyString())).thenAnswer(inv -> properties.get(inv.getArgument(0)));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceId_whenCallerSendsNone() {
        when(request.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null);

        filter.filter(request);

        String generated = MDC.get(TraceIdFilter.MDC_KEY);
        assertNotNull(generated);
        assertDoesNotThrow(() -> UUID.fromString(generated));
    }

    @Test
    void generatesTraceId_whenInboundHeaderIsBlank() {
        when(request.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("   ");

        filter.filter(request);

        assertNotNull(MDC.get(TraceIdFilter.MDC_KEY));
        assertDoesNotThrow(() -> UUID.fromString(MDC.get(TraceIdFilter.MDC_KEY)));
    }

    @Test
    void honoursInboundTraceId() {
        when(request.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("trace-from-gateway");

        filter.filter(request);

        assertEquals("trace-from-gateway", MDC.get(TraceIdFilter.MDC_KEY));
    }

    @Test
    void echoesTraceIdOnResponseAndClearsMdc() {
        when(request.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("trace-from-gateway");

        filter.filter(request);
        filter.filter(request, response);

        assertEquals("trace-from-gateway", responseHeaders.getFirst(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_KEY), "MDC must not leak into the next request on this thread");
    }

    @Test
    void responseFilter_withoutTraceId_addsNoHeaderAndDoesNotThrow() {
        assertDoesNotThrow(() -> filter.filter(request, response));

        assertNull(responseHeaders.getFirst(TraceIdFilter.TRACE_ID_HEADER));
    }
}
