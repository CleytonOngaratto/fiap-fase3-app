package br.com.fiap.postech.carworkshop.shared.infrastructure.observability;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.util.UUID;

@Provider
public class TraceIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Reaproveitar o id de entrada é o que mantém a correlação ponta a ponta: o API Gateway e a
        // Lambda ficam na frente da app, e gerar um novo aqui recomeçaria a trilha na borda.
        String inbound = requestContext.getHeaderString(TRACE_ID_HEADER);
        String traceId = (inbound == null || inbound.isBlank())
                ? UUID.randomUUID().toString()
                : inbound;
        MDC.put(MDC_KEY, traceId);
        requestContext.setProperty(MDC_KEY, traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object traceId = requestContext.getProperty(MDC_KEY);
        if (traceId != null) {
            responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);
        }
        // Threads são reusadas do pool: um MDC residual carimbaria a PRÓXIMA requisição com este
        // traceId. Limpa sempre, inclusive quando esta requisição não chegou a definir um.
        MDC.remove(MDC_KEY);
    }
}
