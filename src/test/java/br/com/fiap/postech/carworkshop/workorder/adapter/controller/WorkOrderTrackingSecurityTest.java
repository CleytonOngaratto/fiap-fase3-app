package br.com.fiap.postech.carworkshop.workorder.adapter.controller;

import br.com.fiap.postech.carworkshop.utils.TokenUtils;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

/**
 * F10. Ao contrário de {@link CustomerTrackingControllerTest}, que simula a identidade com
 * {@code @TestSecurity}, aqui os tokens são assinados de verdade: o de CUSTOMER tem a forma exata
 * que a Lambda do Bloco 5 emite, então este teste prova que os dois repos concordam no contrato —
 * e não apenas que a anotação está no controller.
 */
@QuarkusTest
class WorkOrderTrackingSecurityTest {

    private static final String CPF = "52998224725";

    @InjectMock
    WorkOrderUseCase useCase;

    private WorkOrderTrackingResponse pendingResponse;

    @BeforeEach
    void setUp() {
        pendingResponse = new WorkOrderTrackingResponse(1L, StatusWO.PENDING_APPROVAL, "ABC1234",
                "Troca de óleo necessária", new BigDecimal("250.00"), List.of(), List.of(), true);
    }

    @Test
    void getStatus_withoutToken_isUnauthorized() {
        given().contentType(MediaType.APPLICATION_JSON)
                .when().get("/tracking/1")
                .then().statusCode(401);
    }

    @Test
    void approve_withoutToken_isUnauthorized() {
        given().contentType(MediaType.APPLICATION_JSON)
                .when().post("/tracking/1/approve")
                .then().statusCode(401);
    }

    @Test
    void reject_withoutToken_isUnauthorized() {
        given().contentType(MediaType.APPLICATION_JSON)
                .when().post("/tracking/1/reject")
                .then().statusCode(401);
    }

    @Test
    void getStatus_withCustomerTokenFromLambda_isAllowed() {
        when(useCase.findForCustomer(1L)).thenReturn(pendingResponse);

        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateCustomerToken(CPF))
                .when().get("/tracking/1")
                .then().statusCode(200).body("id", is(1)).body("status", is("PENDING_APPROVAL"));
    }

    @Test
    void approve_withCustomerTokenFromLambda_isAllowed() {
        WorkOrderTrackingResponse inProgress = new WorkOrderTrackingResponse(1L, StatusWO.IN_PROGRESS,
                "ABC1234", null, new BigDecimal("250.00"), List.of(), List.of(), false);
        when(useCase.approveWorkOrder(1L)).thenReturn(inProgress);

        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateCustomerToken(CPF))
                .when().post("/tracking/1/approve")
                .then().statusCode(200).body("status", is("IN_PROGRESS"));
    }

    @Test
    void reject_withCustomerTokenFromLambda_isAllowed() {
        WorkOrderTrackingResponse canceled = new WorkOrderTrackingResponse(1L, StatusWO.CANCELED,
                "ABC1234", null, new BigDecimal("250.00"), List.of(), List.of(), false);
        when(useCase.rejectWorkOrder(1L)).thenReturn(canceled);

        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateCustomerToken(CPF))
                .when().post("/tracking/1/reject")
                .then().statusCode(200).body("status", is("CANCELED"));
    }

    @Test
    void getStatus_withAdminToken_isAllowed() {
        when(useCase.findForCustomer(1L)).thenReturn(pendingResponse);

        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when().get("/tracking/1")
                .then().statusCode(200).body("id", is(1));
    }

    @Test
    void getStatus_withUserRole_isForbidden() {
        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateUserToken())
                .when().get("/tracking/1")
                .then().statusCode(403);
    }

    @Test
    void approve_withUserRole_isForbidden() {
        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TokenUtils.generateUserToken())
                .when().post("/tracking/1/approve")
                .then().statusCode(403);
    }

    @Test
    void getStatus_withGarbageToken_isUnauthorized() {
        given().contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer not-a-jwt")
                .when().get("/tracking/1")
                .then().statusCode(401);
    }
}
