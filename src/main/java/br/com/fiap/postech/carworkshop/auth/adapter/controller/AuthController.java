package br.com.fiap.postech.carworkshop.auth.adapter.controller;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.AuthTokenResponse;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.UserResponse;
import br.com.fiap.postech.carworkshop.auth.usecase.port.in.AuthUseCase;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Tag(name = "Authentication")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthUseCase useCase;

    @POST
    @PermitAll
    @Path("/signup")
    @Operation(summary = "Register a new user account")
    public Response signup(SignupRequest request) {
        UserResponse created = useCase.signup(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @PermitAll
    @Path("/login")
    @Operation(summary = "Authenticate and obtain a JWT token")
    public Response login(LoginRequest request) {
        AuthTokenResponse token = useCase.login(request);
        return Response.ok(token).build();
    }
}
