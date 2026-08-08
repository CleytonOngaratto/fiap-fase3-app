package br.com.fiap.postech.carworkshop.inventory.adapter.controller;

import br.com.fiap.postech.carworkshop.inventory.adapter.dto.PartsAndSupplyRequest;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.in.InventoryUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/parts-and-supplies")
@Tag(name = "Parts and Supplies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartsAndSupplyController {

    @Inject
    InventoryUseCase useCase;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "List all parts and supplies")
    public Response findAll() {
        List<PartsAndSupplyResponse> parts = useCase.findAll();
        return Response.ok(parts).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get part/supply by ID")
    public Response findById(@Parameter(description = "Part ID") @PathParam("id") Long id) {
        PartsAndSupplyResponse part = useCase.findById(id);
        return Response.ok(part).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new part/supply")
    public Response create(PartsAndSupplyRequest request) {
        PartsAndSupplyResponse created = useCase.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update part/supply data")
    public Response update(@Parameter(description = "Part ID") @PathParam("id") Long id,
                           PartsAndSupplyRequest request) {
        useCase.update(id, request);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a part/supply")
    public Response delete(@Parameter(description = "Part ID") @PathParam("id") Long id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/order")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Consume stock for a part/supply")
    public Response consumeStock(@Parameter(description = "Part ID") @PathParam("id") Long id,
                                  @QueryParam("quantity") Integer quantity) {
        useCase.consumeStock(id, quantity);
        return Response.noContent().build();
    }
}
