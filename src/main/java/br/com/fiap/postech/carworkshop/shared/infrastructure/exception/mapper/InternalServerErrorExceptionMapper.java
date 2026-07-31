package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InternalServerErrorException e) {
        var error = StandardError.of(Response.Status.INTERNAL_SERVER_ERROR, e, uriInfo);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
}
