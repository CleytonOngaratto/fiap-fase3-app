package br.com.fiap.postech.carworkshop.autoservice.adapter.controller;

import br.com.fiap.postech.carworkshop.autoservice.adapter.dto.AutoServiceRequest;
import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.in.AutoServiceUseCase;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class AutoServiceControllerTest {

    @InjectMock
    AutoServiceUseCase useCase;

    private AutoServiceResponse response;
    private AutoServiceRequest request;

    @BeforeEach
    void setUp() {
        response = new AutoServiceResponse(1L, "Oil Change", new BigDecimal("99.99"));
        request = new AutoServiceRequest("Oil Change", new BigDecimal("99.99"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findAllServices_Success() {
        Mockito.when(useCase.findAll()).thenReturn(List.of(response));

        given().when().get("/service").then()
                .statusCode(200)
                .body("[0].description", is("Oil Change"))
                .body("[0].id", is(1));

        Mockito.verify(useCase).findAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findAllServices_EmptyList() {
        Mockito.when(useCase.findAll()).thenReturn(Collections.emptyList());
        given().when().get("/service").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void findAllServices_Unauthorized() {
        given().when().get("/service").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void findAllServices_Forbidden() {
        given().when().get("/service").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findServiceById_Success() {
        Mockito.when(useCase.findById(1L)).thenReturn(response);
        given().when().get("/service/1").then()
                .statusCode(200).body("id", is(1)).body("description", is("Oil Change"));
        Mockito.verify(useCase).findById(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findServiceById_NotFound() {
        Mockito.when(useCase.findById(999L)).thenThrow(new EntityNotFoundException("Service not found."));
        given().when().get("/service/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findServiceById_InvalidId_Returns400() {
        Mockito.when(useCase.findById(0L)).thenThrow(new ValidationException("Invalid ID: must be a positive number."));
        given().when().get("/service/0").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createService_Success() {
        Mockito.when(useCase.create(any(AutoServiceRequest.class))).thenReturn(response);
        given().contentType(MediaType.APPLICATION_JSON).body(request).when().post("/service").then()
                .statusCode(201).body("id", is(1)).body("description", is("Oil Change"));
        Mockito.verify(useCase).create(any(AutoServiceRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createService_NullDescription_Returns400() {
        Mockito.when(useCase.create(any(AutoServiceRequest.class)))
                .thenThrow(new ValidationException("Service description must not be blank."));
        var invalid = new AutoServiceRequest(null, new BigDecimal("99.99"));
        given().contentType(MediaType.APPLICATION_JSON).body(invalid).when().post("/service").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createService_NullPrice_Returns400() {
        Mockito.when(useCase.create(any(AutoServiceRequest.class)))
                .thenThrow(new ValidationException("Service price must be greater than zero."));
        var invalid = new AutoServiceRequest("Oil Change", null);
        given().contentType(MediaType.APPLICATION_JSON).body(invalid).when().post("/service").then().statusCode(400);
    }

    @Test
    void createService_Unauthorized() {
        given().contentType(MediaType.APPLICATION_JSON).body(request).when().post("/service").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateService_Success() {
        Mockito.doNothing().when(useCase).update(eq(1L), any(AutoServiceRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(request).when().put("/service/1").then().statusCode(204);
        Mockito.verify(useCase).update(eq(1L), any(AutoServiceRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateService_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Service not found."))
                .when(useCase).update(eq(999L), any(AutoServiceRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(request).when().put("/service/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateService_InvalidId_Returns400() {
        Mockito.doThrow(new ValidationException("Invalid ID.")).when(useCase).update(eq(0L), any(AutoServiceRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(request).when().put("/service/0").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteService_Success() {
        Mockito.doNothing().when(useCase).delete(1L);
        given().when().delete("/service/1").then().statusCode(204);
        Mockito.verify(useCase).delete(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteService_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Service not found.")).when(useCase).delete(999L);
        given().when().delete("/service/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteService_InvalidId_Returns400() {
        Mockito.doThrow(new ValidationException("Invalid ID.")).when(useCase).delete(0L);
        given().when().delete("/service/0").then().statusCode(400);
    }

    @Test
    void deleteService_Unauthorized() {
        given().when().delete("/service/1").then().statusCode(401);
    }
}
