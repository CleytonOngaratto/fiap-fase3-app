package br.com.fiap.postech.carworkshop.workorder.adapter.controller;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.DiagnosisRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.WorkOrderRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.DiagnosisResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderDetailResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class WorkOrderControllerTest {

    @InjectMock
    WorkOrderUseCase useCase;

    private WorkOrderRequest workOrderRequest;
    private WorkOrderResponse workOrderResponse;
    private DiagnosisResponse diagnosisResponse;
    private WorkOrderDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        workOrderRequest = new WorkOrderRequest();
        workOrderRequest.setCustomerId(1L);
        workOrderRequest.setVehicleId(1L);
        workOrderRequest.setServiceIds(List.of(1L, 2L));

        workOrderResponse = new WorkOrderResponse(1L, 1L, 1L, StatusWO.RECEIVED, null, null, List.of(), List.of());
        diagnosisResponse = new DiagnosisResponse(1L, StatusWO.PENDING_APPROVAL,
                "Necessário troca de óleo e filtros", new BigDecimal("350.00"));
        detailResponse = new WorkOrderDetailResponse(1L, 1L, 1L, StatusWO.RECEIVED, null, null, null, null, List.of(), List.of());
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateWorkOrder_Success() {
        Mockito.when(useCase.create(any(WorkOrderRequest.class))).thenReturn(workOrderResponse);

        given().contentType(MediaType.APPLICATION_JSON).body(workOrderRequest)
                .when().post("/work-orders").then()
                .statusCode(201).body("id", is(1)).body("status", is("RECEIVED"));

        Mockito.verify(useCase).create(any(WorkOrderRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateWorkOrder_WithoutServices() {
        Mockito.when(useCase.create(any(WorkOrderRequest.class)))
                .thenThrow(new ValidationException("A work order must have at least one service."));

        WorkOrderRequest invalidRequest = new WorkOrderRequest();
        invalidRequest.setCustomerId(1L);
        invalidRequest.setVehicleId(1L);
        invalidRequest.setServiceIds(List.of());

        given().contentType(MediaType.APPLICATION_JSON).body(invalidRequest)
                .when().post("/work-orders").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCompleteDiagnosis_Success() {
        DiagnosisRequest diagnosisRequest = new DiagnosisRequest();
        diagnosisRequest.setDiagnosticDescription("Necessário troca de óleo e filtros");

        Mockito.when(useCase.completeDiagnosis(eq(1L), any(DiagnosisRequest.class))).thenReturn(diagnosisResponse);

        given().contentType(MediaType.APPLICATION_JSON).body(diagnosisRequest)
                .when().post("/work-orders/1/complete-diagnosis").then()
                .statusCode(200).body("id", is(1)).body("status", is("PENDING_APPROVAL"))
                .body("diagnostic_description", is("Necessário troca de óleo e filtros"));

        Mockito.verify(useCase).completeDiagnosis(eq(1L), any(DiagnosisRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCompleteDiagnosis_WorkOrderNotFound() {
        DiagnosisRequest diagnosisRequest = new DiagnosisRequest();
        diagnosisRequest.setDiagnosticDescription("Teste");

        Mockito.when(useCase.completeDiagnosis(eq(999L), any(DiagnosisRequest.class)))
                .thenThrow(new EntityNotFoundException("Work Order not found."));

        given().contentType(MediaType.APPLICATION_JSON).body(diagnosisRequest)
                .when().post("/work-orders/999/complete-diagnosis").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCompleteWorkOrder_Success() {
        WorkOrderDetailResponse completed = new WorkOrderDetailResponse(1L, 1L, 1L, StatusWO.COMPLETED, null, null, null, null, List.of(), List.of());
        Mockito.when(useCase.completeWorkOrder(1L)).thenReturn(completed);

        given().contentType(MediaType.APPLICATION_JSON)
                .when().patch("/work-orders/1/complete").then()
                .statusCode(200).body("id", is(1)).body("status", is("COMPLETED"));

        Mockito.verify(useCase).completeWorkOrder(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCompleteWorkOrder_NotFound() {
        Mockito.when(useCase.completeWorkOrder(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().contentType(MediaType.APPLICATION_JSON).when().patch("/work-orders/999/complete").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeliverWorkOrder_Success() {
        WorkOrderDetailResponse delivered = new WorkOrderDetailResponse(1L, 1L, 1L, StatusWO.DELIVERED, null, null, null, null, List.of(), List.of());
        Mockito.when(useCase.deliverWorkOrder(1L)).thenReturn(delivered);

        given().contentType(MediaType.APPLICATION_JSON)
                .when().patch("/work-orders/1/deliver").then()
                .statusCode(200).body("id", is(1)).body("status", is("DELIVERED"));

        Mockito.verify(useCase).deliverWorkOrder(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeliverWorkOrder_NotFound() {
        Mockito.when(useCase.deliverWorkOrder(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().contentType(MediaType.APPLICATION_JSON).when().patch("/work-orders/999/deliver").then().statusCode(404);
    }

    @Test
    void testCreateWorkOrder_Unauthorized() {
        given().contentType(MediaType.APPLICATION_JSON).body(workOrderRequest)
                .when().post("/work-orders").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "user", roles = "USER")
    void testCreateWorkOrder_Forbidden() {
        given().contentType(MediaType.APPLICATION_JSON).body(workOrderRequest)
                .when().post("/work-orders").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testGetAllWorkOrders_Success() {
        WorkOrderDetailResponse detail = new WorkOrderDetailResponse(1L, 1L, 1L, StatusWO.RECEIVED, null, null, null, null, List.of(), List.of());
        Mockito.when(useCase.findAll()).thenReturn(List.of(detail));

        given().when().get("/work-orders").then()
                .statusCode(200).body("size()", is(1))
                .body("[0].id", is(1)).body("[0].status", is("RECEIVED"));

        Mockito.verify(useCase).findAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testGetWorkOrderById_Success() {
        WorkOrderDetailResponse detail = new WorkOrderDetailResponse(1L, 1L, 1L, StatusWO.IN_PROGRESS, null, null, null, null, List.of(), List.of());
        Mockito.when(useCase.findById(1L)).thenReturn(detail);

        given().when().get("/work-orders/1").then()
                .statusCode(200).body("id", is(1)).body("status", is("IN_PROGRESS"));

        Mockito.verify(useCase).findById(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testGetWorkOrderById_NotFound() {
        Mockito.when(useCase.findById(999L)).thenThrow(new EntityNotFoundException("Work Order not found."));
        given().when().get("/work-orders/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateWorkOrder_NullBody_Returns400() {
        given().contentType(MediaType.APPLICATION_JSON).when().post("/work-orders").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCompleteDiagnosis_NullBody_Returns400() {
        given().contentType(MediaType.APPLICATION_JSON).when().post("/work-orders/1/complete-diagnosis").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testGetAverageCompletionTime_Success() {
        Mockito.when(useCase.getAverageCompletionTimeInHours()).thenReturn(4.5);

        given().when().get("/work-orders/stats/average-completion-time").then()
                .statusCode(200).body("average_hours", is(4.5f));

        Mockito.verify(useCase).getAverageCompletionTimeInHours();
    }
}
