package br.com.fiap.postech.carworkshop.vehicle.adapter.controller;

import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.in.VehicleUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/vehicles")
@Tag(name = "Vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleController {

    @Inject
    VehicleUseCase useCase;

    @GET
    @Path("/get-all")
    @RolesAllowed("ADMIN")
    @Operation(summary = "List all vehicles")
    public Response findAll() {
        List<VehicleResponse> vehicles = useCase.findAll();
        return Response.ok(vehicles).build();
    }

    @GET
    @Path("/get-by-id/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get vehicle by ID")
    public Response findById(@Parameter(description = "Vehicle ID") @PathParam("id") Long id) {
        VehicleResponse vehicle = useCase.findById(id);
        return Response.ok(vehicle).build();
    }

    @GET
    @Path("/get-by-plate/{plate}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get vehicle by license plate")
    public Response findByPlate(@Parameter(description = "License plate") @PathParam("plate") String plate) {
        VehicleResponse vehicle = useCase.findByVehiclePlate(plate);
        return Response.ok(vehicle).build();
    }

    @POST
    @Path("/create")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new vehicle")
    public Response create(VehicleRequest request) {
        VehicleResponse created = useCase.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/update/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update vehicle data")
    public Response update(@Parameter(description = "Vehicle ID") @PathParam("id") Long id,
                           VehicleRequest request) {
        useCase.update(id, request);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a vehicle")
    public Response delete(@Parameter(description = "Vehicle ID") @PathParam("id") Long id) {
        useCase.delete(id);
        return Response.noContent().build();
    }
}
