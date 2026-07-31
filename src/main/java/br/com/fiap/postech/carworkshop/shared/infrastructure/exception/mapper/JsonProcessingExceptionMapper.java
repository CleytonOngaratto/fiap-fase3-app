package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Catches low-level Jackson read failures (e.g. malformed JSON: trailing comma, unbalanced braces)
 * that surface as {@link JsonProcessingException} but are NOT {@code JsonMappingException}. Without
 * this, such requests fell through to the Quarkus default and returned a 400 with an empty body.
 *
 * <p>JAX-RS picks the most specific mapper, so databind/unknown-field errors still go to
 * {@link JsonMappingExceptionMapper}; only pure parse/stream errors land here.</p>
 */
@Slf4j
@Provider
@ApplicationScoped
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(JsonProcessingException e) {
        var error = StandardError.builder()
                .timestamp(java.time.Instant.now())
                .status(400)
                .error(e.getOriginalMessage())
                .path(uriInfo.getRequestUri().toString())
                .build();
        log.info(error.toString());
        return Response.status(400).entity(error).build();
    }
}
