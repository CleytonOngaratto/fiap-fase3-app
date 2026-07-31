package br.com.fiap.postech.carworkshop.customer.adapter.controller;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;
import br.com.fiap.postech.carworkshop.customer.usecase.port.in.CustomerUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/customers")
@Tag(name = "Customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {

    @Inject
    CustomerUseCase useCase;

    @GET
    @Path("/get-all")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Find all customers")
    public Response findAll() {
        List<CustomerResponse> customers = useCase.findAll();
        return Response.ok(customers).build();
    }

    @GET
    @Path("/get-by-id/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Find customer by ID")
    public Response findById(@Parameter(description = "Customer ID") @PathParam("id") Long id) {
        CustomerResponse customer = useCase.findById(id);
        return Response.ok(customer).build();
    }

    @GET
    @Path("/get-by-document/{document}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Find customer by CPF/CNPJ document")
    public Response findByDocument(@Parameter(description = "CPF or CNPJ") @PathParam("document") String document) {
        CustomerResponse customer = useCase.findByDocument(document);
        return Response.ok(customer).build();
    }

    @GET
    @Path("/get-by-email/{email}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Find customer by e-mail")
    public Response findByEmail(@Parameter(description = "Customer e-mail") @PathParam("email") String email) {
        CustomerResponse customer = useCase.findByEmail(email);
        return Response.ok(customer).build();
    }

    @POST
    @Path("/create")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new customer")
    public Response create(CustomerRequest request) {
        CustomerResponse created = useCase.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/update/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update customer data")
    public Response update(@Parameter(description = "Customer ID") @PathParam("id") Long id,
                           CustomerRequest request) {
        useCase.update(id, request);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a customer")
    public Response delete(@Parameter(description = "Customer ID") @PathParam("id") Long id) {
        useCase.delete(id);
        return Response.noContent().build();
    }
}
