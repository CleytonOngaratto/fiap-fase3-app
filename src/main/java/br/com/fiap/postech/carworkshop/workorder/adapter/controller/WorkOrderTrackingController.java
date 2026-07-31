package br.com.fiap.postech.carworkshop.workorder.adapter.controller;

import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/tracking")
@Tag(name = "Customer Tracking")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkOrderTrackingController {

    @Inject
    WorkOrderUseCase useCase;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get work order status and details")
    public Response getStatus(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderTrackingResponse response = useCase.findForCustomer(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/approve")
    @Operation(summary = "Approve budget for a work order")
    public Response approve(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderTrackingResponse response = useCase.approveWorkOrder(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/reject")
    @Operation(summary = "Reject budget for a work order")
    public Response reject(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderTrackingResponse response = useCase.rejectWorkOrder(id);
        return Response.ok(response).build();
    }
}
