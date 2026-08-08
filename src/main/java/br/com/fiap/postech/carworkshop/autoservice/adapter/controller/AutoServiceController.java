package br.com.fiap.postech.carworkshop.autoservice.adapter.controller;

import br.com.fiap.postech.carworkshop.autoservice.adapter.dto.AutoServiceRequest;
import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.in.AutoServiceUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/service")
@Tag(name = "Auto Services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutoServiceController {

    @Inject
    AutoServiceUseCase useCase;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "List all auto services")
    public Response findAll() {
        List<AutoServiceResponse> services = useCase.findAll();
        return Response.ok(services).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("/{id}")
    @Operation(summary = "Get an auto service by ID")
    public Response findById(@Parameter(description = "Service ID") @PathParam("id") Long id) {
        AutoServiceResponse service = useCase.findById(id);
        return Response.ok(service).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new auto service")
    public Response create(AutoServiceRequest request) {
        AutoServiceResponse created = useCase.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @RolesAllowed("ADMIN")
    @Path("/{id}")
    @Operation(summary = "Update an auto service")
    public Response update(@Parameter(description = "Service ID") @PathParam("id") Long id,
                           AutoServiceRequest request) {
        useCase.update(id, request);
        return Response.noContent().build();
    }

    @DELETE
    @RolesAllowed("ADMIN")
    @Path("/{id}")
    @Operation(summary = "Delete an auto service")
    public Response delete(@Parameter(description = "Service ID") @PathParam("id") Long id) {
        useCase.delete(id);
        return Response.noContent().build();
    }
}
