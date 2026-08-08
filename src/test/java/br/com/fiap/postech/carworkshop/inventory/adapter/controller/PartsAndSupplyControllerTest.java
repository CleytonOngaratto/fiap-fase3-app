package br.com.fiap.postech.carworkshop.inventory.adapter.controller;

import br.com.fiap.postech.carworkshop.inventory.adapter.dto.PartsAndSupplyRequest;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.in.InventoryUseCase;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.StockException;
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
class PartsAndSupplyControllerTest {

    @InjectMock
    InventoryUseCase useCase;

    private PartsAndSupplyResponse response;
    private PartsAndSupplyRequest request;

    @BeforeEach
    void setUp() {
        response = new PartsAndSupplyResponse(1L, "P001", "Bosch", "Oil Filter",
                new BigDecimal("35.90"), TypeProductEnum.UNITARY, 10);
        request = new PartsAndSupplyRequest("P001", "Bosch", "Oil Filter",
                new BigDecimal("35.90"), TypeProductEnum.UNITARY, 10);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findAllPartsAndSupplies_Success() {
        Mockito.when(useCase.findAll()).thenReturn(List.of(response));
        given().when().get("/parts-and-supplies").then()
                .statusCode(200).body("[0].code", is("P001")).body("[0].manufacturer", is("Bosch"));
        Mockito.verify(useCase).findAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findAllPartsAndSupplies_EmptyList() {
        Mockito.when(useCase.findAll()).thenReturn(Collections.emptyList());
        given().when().get("/parts-and-supplies").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void findAllPartsAndSupplies_Unauthorized() {
        given().when().get("/parts-and-supplies").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void findAllPartsAndSupplies_Forbidden() {
        given().when().get("/parts-and-supplies").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findPartsAndSupplyById_Success() {
        Mockito.when(useCase.findById(1L)).thenReturn(response);
        given().when().get("/parts-and-supplies/1").then()
                .statusCode(200).body("id", is(1)).body("code", is("P001"))
                .body("manufacturer", is("Bosch")).body("description", is("Oil Filter"))
                .body("type", is("UNITARY")).body("quantity", is(10));
        Mockito.verify(useCase).findById(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findPartsAndSupplyById_NotFound() {
        Mockito.when(useCase.findById(999L)).thenThrow(new EntityNotFoundException("Part or Supply not found."));
        given().when().get("/parts-and-supplies/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void findPartsAndSupplyById_InvalidId() {
        Mockito.when(useCase.findById(0L)).thenThrow(new ValidationException("Invalid ID: must be a positive number."));
        given().when().get("/parts-and-supplies/0").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createPartsAndSupply_Success() {
        Mockito.when(useCase.create(any(PartsAndSupplyRequest.class))).thenReturn(response);
        given().contentType(MediaType.APPLICATION_JSON).body(request)
                .when().post("/parts-and-supplies").then()
                .statusCode(201).body("id", is(1)).body("code", is("P001"));
        Mockito.verify(useCase).create(any(PartsAndSupplyRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createPartsAndSupply_MissingRequiredField_Returns400() {
        Mockito.when(useCase.create(any(PartsAndSupplyRequest.class)))
                .thenThrow(new ValidationException("Part description must not be blank."));
        var invalid = new PartsAndSupplyRequest(null, "Bosch", null, new BigDecimal("35.90"), TypeProductEnum.UNITARY, 10);
        given().contentType(MediaType.APPLICATION_JSON).body(invalid)
                .when().post("/parts-and-supplies").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createPartsAndSupply_NegativePrice_Returns400() {
        Mockito.when(useCase.create(any(PartsAndSupplyRequest.class)))
                .thenThrow(new ValidationException("Part price must be greater than zero."));
        var invalid = new PartsAndSupplyRequest("P001", "Bosch", "Oil Filter",
                new BigDecimal("-1.00"), TypeProductEnum.UNITARY, 10);
        given().contentType(MediaType.APPLICATION_JSON).body(invalid)
                .when().post("/parts-and-supplies").then().statusCode(400);
    }

    @Test
    void createPartsAndSupply_Unauthorized() {
        given().contentType(MediaType.APPLICATION_JSON).body(request)
                .when().post("/parts-and-supplies").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updatePartsAndSupply_Success() {
        Mockito.doNothing().when(useCase).update(eq(1L), any(PartsAndSupplyRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(request)
                .when().put("/parts-and-supplies/1").then().statusCode(204);
        Mockito.verify(useCase).update(eq(1L), any(PartsAndSupplyRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updatePartsAndSupply_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Part or Supply not found."))
                .when(useCase).update(eq(999L), any(PartsAndSupplyRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(request)
                .when().put("/parts-and-supplies/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deletePartsAndSupply_Success() {
        Mockito.doNothing().when(useCase).delete(1L);
        given().when().delete("/parts-and-supplies/1").then().statusCode(204);
        Mockito.verify(useCase).delete(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deletePartsAndSupply_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Part or Supply not found.")).when(useCase).delete(999L);
        given().when().delete("/parts-and-supplies/999").then().statusCode(404);
    }

    @Test
    void deletePartsAndSupply_Unauthorized() {
        given().when().delete("/parts-and-supplies/1").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void consumeStock_Success() {
        Mockito.doNothing().when(useCase).consumeStock(1L, 5);
        given().queryParam("quantity", 5).when().patch("/parts-and-supplies/1/order").then().statusCode(204);
        Mockito.verify(useCase).consumeStock(1L, 5);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void consumeStock_InsufficientStock_Returns422() {
        Mockito.doThrow(new StockException("Insufficient stock.")).when(useCase).consumeStock(1L, 100);
        given().queryParam("quantity", 100).when().patch("/parts-and-supplies/1/order").then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void consumeStock_InvalidQuantity_Returns400() {
        Mockito.doThrow(new ValidationException("Quantity must be a positive integer.")).when(useCase).consumeStock(1L, 0);
        given().queryParam("quantity", 0).when().patch("/parts-and-supplies/1/order").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void consumeStock_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Part or Supply not found.")).when(useCase).consumeStock(999L, 1);
        given().queryParam("quantity", 1).when().patch("/parts-and-supplies/999/order").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "customer", roles = "CUSTOMER")
    void consumeStock_Forbidden() {
        given().queryParam("quantity", 1).when().patch("/parts-and-supplies/1/order").then().statusCode(403);
    }
}
