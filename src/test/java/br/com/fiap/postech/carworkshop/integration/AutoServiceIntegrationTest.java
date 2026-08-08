package br.com.fiap.postech.carworkshop.integration;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class AutoServiceIntegrationTest {

    @Test
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void findAllServices() {

        given()
                .when()
                .get("/service")
                .then()
                .body("size()", equalTo(2))
                .body("id", hasItems(1, 2))
                .body("description", hasItems("Troca de Óleo", "Alinhamento"))
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void findServiceById() {
        given()
                .when()
                .get("/service/1")
                .then()
                .body("id", equalTo(1))
                .body("description", equalTo("Troca de Óleo"))
                .body("price", equalTo(299.99F))
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void createService() {
        JsonObject jsonObject =
                Json.createObjectBuilder()
                        .add("description", "Troca de Pastilhas de Freio")
                        .add("price", "399.99")
                        .build();

        given()
                .contentType("application/json")
                .body(jsonObject.toString())
                .when()
                .post("/service")
                .then()
                .body("id", notNullValue())
                .body("description", equalTo("Troca de Pastilhas de Freio"))
                .body("price", equalTo(399.99F))
                .statusCode(Response.Status.CREATED.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void updateService() {

        JsonObject jsonObject =
                Json.createObjectBuilder()
                        .add("id", 1L)
                        .add("description", "Troca de Óleo")
                        .add("price", "499.99")
                        .build();

        given()
                .contentType("application/json")
                .body(jsonObject.toString())
                .when()
                .put("/service/1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void deleteService() {
        given()
                .when()
                .delete("/service/1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
