package br.com.fiap.postech.carworkshop.vehicle.adapter.controller;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.adapter.presenter.VehicleResponse;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.in.VehicleUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class VehicleControllerTest {

    @InjectMock
    VehicleUseCase useCase;

    private VehicleResponse vehicleResponse;
    private VehicleRequest vehicleRequest;

    @BeforeEach
    void setUp() {
        vehicleResponse = new VehicleResponse(1L, "ABC-1234", "Toyota", "Corolla", 2020, 1L);
        vehicleRequest = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, 1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindAllVehicles_Success() {
        Mockito.when(useCase.findAll()).thenReturn(List.of(vehicleResponse));
        given().when().get("/vehicles/get-all").then()
                .statusCode(200).body("[0].vehiclePlate", is("ABC-1234"));
        Mockito.verify(useCase).findAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindAllVehicles_Empty() {
        Mockito.when(useCase.findAll()).thenReturn(List.of());
        given().when().get("/vehicles/get-all").then().statusCode(200).body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindVehicleById_Success() {
        Mockito.when(useCase.findById(1L)).thenReturn(vehicleResponse);
        given().when().get("/vehicles/get-by-id/1").then()
                .statusCode(200).body("id", is(1)).body("vehiclePlate", is("ABC-1234"));
        Mockito.verify(useCase).findById(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindVehicleById_NotFound() {
        Mockito.when(useCase.findById(999L)).thenThrow(new EntityNotFoundException("Vehicle not found."));
        given().when().get("/vehicles/get-by-id/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindVehicleByPlate_Success() {
        Mockito.when(useCase.findByVehiclePlate("ABC-1234")).thenReturn(vehicleResponse);
        given().when().get("/vehicles/get-by-plate/ABC-1234").then()
                .statusCode(200).body("vehiclePlate", is("ABC-1234"));
        Mockito.verify(useCase).findByVehiclePlate("ABC-1234");
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindVehicleByPlate_NotFound() {
        Mockito.when(useCase.findByVehiclePlate("XYZ-9999")).thenThrow(new EntityNotFoundException("Vehicle not found."));
        given().when().get("/vehicles/get-by-plate/XYZ-9999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateVehicle_Success() {
        Mockito.when(useCase.create(any(VehicleRequest.class))).thenReturn(vehicleResponse);
        given().contentType(MediaType.APPLICATION_JSON).body(vehicleRequest)
                .when().post("/vehicles/create").then()
                .statusCode(201).body("id", is(1)).body("vehiclePlate", is("ABC-1234"));
        Mockito.verify(useCase).create(any(VehicleRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateVehicle_PlateAlreadyExists() {
        Mockito.when(useCase.create(any(VehicleRequest.class)))
                .thenThrow(new ValidationException("Vehicle plate already exists."));
        given().contentType(MediaType.APPLICATION_JSON).body(vehicleRequest)
                .when().post("/vehicles/create").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testUpdateVehicle_Success() {
        Mockito.doNothing().when(useCase).update(eq(1L), any(VehicleRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(vehicleRequest)
                .when().put("/vehicles/update/1").then().statusCode(204);
        Mockito.verify(useCase).update(eq(1L), any(VehicleRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testUpdateVehicle_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Vehicle not found."))
                .when(useCase).update(eq(999L), any(VehicleRequest.class));
        given().contentType(MediaType.APPLICATION_JSON).body(vehicleRequest)
                .when().put("/vehicles/update/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeleteVehicle_Success() {
        Mockito.doNothing().when(useCase).delete(1L);
        given().when().delete("/vehicles/delete/1").then().statusCode(204);
        Mockito.verify(useCase).delete(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeleteVehicle_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Vehicle not found.")).when(useCase).delete(999L);
        given().when().delete("/vehicles/delete/999").then().statusCode(404);
    }

    @Test
    void testFindAll_Unauthorized() {
        given().when().get("/vehicles/get-all").then().statusCode(401);
    }
}
