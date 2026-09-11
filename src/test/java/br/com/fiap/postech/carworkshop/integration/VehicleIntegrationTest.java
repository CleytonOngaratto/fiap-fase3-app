package br.com.fiap.postech.carworkshop.integration;

import br.com.fiap.postech.carworkshop.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VehicleIntegrationTest {

    @Test
    @Order(1)
    void findAllVehicles() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .get("/vehicles/get-all")
                .then()
                .body("size()", equalTo(3))
                .body("id", hasItems(1, 2, 3))
                .body("modelName", hasItems("Civic", "Corolla", "Fit"))
                .statusCode(200);
    }

    @Test
    @Order(2)
    void findVehicleById() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .get("/vehicles/get-by-id/1")
                .then()
                .body("id", equalTo(1))
                .body("modelName", equalTo("Corolla"))
                .body("vehiclePlate", equalTo("ABC-1234"))
                .statusCode(200);
    }

    @Test
    @Order(3)
    void testFindVehicleById_should_return_404_when_vehicle_not_found() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .get("/vehicles/get-by-id/99999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void findVehicleByPlate() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .get("/vehicles/get-by-plate/ABC-1234")
                .then()
                .body("id", equalTo(1))
                .body("modelName", equalTo("Corolla"))
                .body("vehiclePlate", equalTo("ABC-1234"))
                .statusCode(200);
    }

    @Test
    @Order(5)
    void testFindVehicleByPlate_should_return_404_when_plate_not_found() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .get("/vehicles/get-by-plate/XYZ-9999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    @Transactional
    void testCreateVehicle_should_return_201() {
        String vehicleJson = """
        {
            "vehiclePlate": "NEW-1234",
            "manufacturer": "Honda",
            "modelName": "Civic Sport",
            "modelYear": 2024,
            "customerId": 1
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(vehicleJson)
                .when()
                .post("/vehicles/create")
                .then()
                .statusCode(201)
                .body("vehiclePlate", equalTo("NEW-1234"))
                .body("manufacturer", equalTo("Honda"))
                .body("modelName", equalTo("Civic Sport"))
                .body("modelYear", equalTo(2024))
                .body("customerId", equalTo(1));
    }

    // D3 — vehicle->owner binding. Orphan creation is rejected and nothing is
    // persisted. The PLAN anticipated "404/422"; the actual contract is 400 (missing owner ->
    // ValidationException) and 404 (unknown owner -> EntityNotFoundException) — same invariant.
    @Test
    @Order(6)
    void testCreateVehicle_should_return_400_when_customerId_missing() {
        String vehicleJson = """
        {
            "vehiclePlate": "MIS-0001",
            "manufacturer": "Honda",
            "modelName": "Fit",
            "modelYear": 2024
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(vehicleJson)
                .when()
                .post("/vehicles/create")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    void testCreateVehicle_should_return_404_when_customer_not_found() {
        String vehicleJson = """
        {
            "vehiclePlate": "NOT-0001",
            "manufacturer": "Honda",
            "modelName": "Fit",
            "modelYear": 2024,
            "customerId": 99999
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(vehicleJson)
                .when()
                .post("/vehicles/create")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    void testCreateVehicle_should_return_400_when_plate_already_exists() {
        String vehicleJson = """
        {
            "vehiclePlate": "ABC-1234",
            "manufacturer": "Toyota",
            "modelName": "Corolla",
            "modelYear": 2020
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(vehicleJson)
                .when()
                .post("/vehicles/create")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    void testCreateVehicle_should_return_400_when_required_field_is_missing() {
        String vehicleJson = """
        {
            "manufacturer": "Toyota",
            "modelName": "Corolla"
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(vehicleJson)
                .when()
                .post("/vehicles/create")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(9)
    @Transactional
    void testUpdateVehicle_should_return_204() {
        String updateJson = """
        {
            "vehiclePlate": "UPD-1234",
            "manufacturer": "Toyota",
            "modelName": "Corolla Updated",
            "modelYear": 2025
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateJson)
                .when()
                .put("/vehicles/update/2")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(10)
    void testUpdateVehicle_should_return_404_when_vehicle_not_found() {
        String updateJson = """
        {
            "vehiclePlate": "UPD-1234",
            "manufacturer": "Toyota",
            "modelName": "Corolla",
            "modelYear": 2025
        }
        """;

        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateJson)
                .when()
                .put("/vehicles/update/99999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @Transactional
    void testDeleteVehicle_should_return_204() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .delete("/vehicles/delete/3")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(12)
    void testDeleteVehicle_should_return_404_when_vehicle_not_found() {
        given()
                .header("Authorization", "Bearer " + TokenUtils.generateAdminToken())
                .when()
                .delete("/vehicles/delete/99999")
                .then()
                .statusCode(404);
    }

}
