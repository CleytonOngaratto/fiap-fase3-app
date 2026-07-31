package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
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
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(EntityNotFoundException e) {
        var error = StandardError.of(Response.Status.NOT_FOUND, e, uriInfo);
        log.info(error.toString());
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }
}
