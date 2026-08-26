package br.com.fiap.postech.carworkshop.workorder.adapter.controller;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.InvalidOperationException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.StockException;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
class CustomerTrackingControllerTest {

    @InjectMock
    WorkOrderUseCase useCase;

    private WorkOrderTrackingResponse pendingResponse;

    @BeforeEach
    void setUp() {
        pendingResponse = new WorkOrderTrackingResponse(1L, StatusWO.PENDING_APPROVAL, "ABC1234",
                "Troca de óleo necessária", new BigDecimal("250.00"), List.of(), List.of(), true);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void getWorkOrderStatus_Success() {
        when(useCase.findForCustomer(1L)).thenReturn(pendingResponse);

        given().contentType(MediaType.APPLICATION_JSON).when().get("/tracking/1").then()
                .statusCode(200).body("id", is(1)).body("status", is("PENDING_APPROVAL"))
                .body("vehicle_plate", is("ABC1234")).body("actions_available", is(true));

        Mockito.verify(useCase).findForCustomer(1L);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void getWorkOrderStatus_NotFound() {
        when(useCase.findForCustomer(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().contentType(MediaType.APPLICATION_JSON).when().get("/tracking/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void approveBudget_Success() {
        WorkOrderTrackingResponse inProgress = new WorkOrderTrackingResponse(1L, StatusWO.IN_PROGRESS, "ABC1234",
                null, new BigDecimal("250.00"), List.of(), List.of(), false);
        when(useCase.approveWorkOrder(1L)).thenReturn(inProgress);

        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/1/approve").then()
                .statusCode(200).body("id", is(1)).body("status", is("IN_PROGRESS"))
                .body("actions_available", is(false));

        Mockito.verify(useCase).approveWorkOrder(1L);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void approveBudget_WorkOrderNotFound() {
        when(useCase.approveWorkOrder(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/999/approve").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void approveBudget_InvalidState() {
        when(useCase.approveWorkOrder(1L))
                .thenThrow(new InvalidOperationException("Budget can only be approved when status is PENDING_APPROVAL."));
        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/1/approve").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void approveBudget_InsufficientStock() {
        when(useCase.approveWorkOrder(1L)).thenThrow(new StockException("Insufficient stock for part: Filtro de óleo"));
        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/1/approve").then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void rejectBudget_Success() {
        WorkOrderTrackingResponse canceled = new WorkOrderTrackingResponse(1L, StatusWO.CANCELED, "ABC1234",
                null, new BigDecimal("250.00"), List.of(), List.of(), false);
        when(useCase.rejectWorkOrder(1L)).thenReturn(canceled);

        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/1/reject").then()
                .statusCode(200).body("id", is(1)).body("status", is("CANCELED"))
                .body("actions_available", is(false));

        Mockito.verify(useCase).rejectWorkOrder(1L);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void rejectBudget_WorkOrderNotFound() {
        when(useCase.rejectWorkOrder(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/999/reject").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void rejectBudget_InvalidState() {
        when(useCase.rejectWorkOrder(1L))
                .thenThrow(new InvalidOperationException("Budget can only be rejected when status is PENDING_APPROVAL."));
        given().contentType(MediaType.APPLICATION_JSON).when().post("/tracking/1/reject").then().statusCode(400);
    }
}
