package br.com.fiap.postech.carworkshop.workorder.adapter.controller;

import br.com.fiap.postech.carworkshop.workorder.adapter.dto.DiagnosisRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.WorkOrderRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.DiagnosisResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderDetailResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/work-orders")
@Tag(name = "Work Orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkOrderController {

    @Inject
    WorkOrderUseCase useCase;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "List all work orders (including deleted)")
    public Response findAll() {
        List<WorkOrderDetailResponse> workOrders = useCase.findAll();
        return Response.ok(workOrders).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("/active")
    @Operation(summary = "List active work orders ordered by priority (Execution > Approval > Diagnosis > Received)")
    public Response findAllActive() {
        List<WorkOrderDetailResponse> workOrders = useCase.findAllActive();
        return Response.ok(workOrders).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("/{id}")
    @Operation(summary = "Get work order by ID")
    public Response findById(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderDetailResponse workOrder = useCase.findById(id);
        return Response.ok(workOrder).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("/stats/average-completion-time")
    @Operation(summary = "Get average completion time in hours")
    public Response getAverageCompletionTime() {
        return Response.ok(Map.of("average_hours", useCase.getAverageCompletionTimeInHours())).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new work order")
    public Response create(WorkOrderRequest request) {
        if (request == null) throw new BadRequestException("Request body is required.");
        WorkOrderResponse created = useCase.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Path("/{id}/complete-diagnosis")
    @Operation(summary = "Complete diagnosis and generate budget")
    public Response completeDiagnosis(@Parameter(description = "Work order ID") @PathParam("id") Long id,
                                       DiagnosisRequest request) {
        if (request == null) throw new BadRequestException("Request body is required.");
        DiagnosisResponse updated = useCase.completeDiagnosis(id, request);
        return Response.ok(updated).build();
    }

    @PATCH
    @RolesAllowed("ADMIN")
    @Path("/{id}/complete")
    @Operation(summary = "Mark work order as completed")
    public Response completeWorkOrder(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderDetailResponse updated = useCase.completeWorkOrder(id);
        return Response.ok(updated).build();
    }

    @PATCH
    @RolesAllowed("ADMIN")
    @Path("/{id}/deliver")
    @Operation(summary = "Mark vehicle as delivered")
    public Response deliverWorkOrder(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        WorkOrderDetailResponse updated = useCase.deliverWorkOrder(id);
        return Response.ok(updated).build();
    }

    @DELETE
    @RolesAllowed("ADMIN")
    @Path("/{id}")
    @Operation(summary = "Soft-delete a completed or delivered work order (logical deletion)")
    public Response deleteWorkOrder(@Parameter(description = "Work order ID") @PathParam("id") Long id) {
        useCase.deleteWorkOrder(id);
        return Response.noContent().build();
    }
}
