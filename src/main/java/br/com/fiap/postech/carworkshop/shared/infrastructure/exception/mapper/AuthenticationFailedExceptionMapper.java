package br.com.fiap.postech.carworkshop.shared.infrastructure.exception.mapper;

import br.com.fiap.postech.carworkshop.shared.infrastructure.exception.StandardError;
import io.quarkus.security.AuthenticationFailedException;
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
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationFailedException e) {
        var error = StandardError.builder()
                .timestamp(java.time.Instant.now())
                .status(401)
                .error("Invalid or expired token")
                .path(uriInfo.getRequestUri().toString())
                .build();
        log.info(error.toString());
        return Response.status(401).entity(error).build();
    }
}
