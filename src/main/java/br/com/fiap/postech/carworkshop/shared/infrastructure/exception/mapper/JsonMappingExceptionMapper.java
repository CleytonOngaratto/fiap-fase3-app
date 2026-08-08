package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@ApplicationScoped
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(JsonMappingException e) {
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
