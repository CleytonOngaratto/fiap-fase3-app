package br.com.fiap.postech.carworkshop.integration;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PartsAndSupplyIntegrationTest {

    @Test
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void findAllPartsAndSupplies() {

        given()
                .when()
                .get("/parts-and-supplies")
                .then()
                .body("size()", equalTo(3))
                .body("id", hasItems(1, 2, 3))
                .body("description", hasItems("Filtro de óleo", "Pastilha de freio", "Filtro de ar"))
                .body("quantity", hasItems(10, 5, 4))
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void findPartsAndSupplyById() {
        given()
                .when()
                .get("/parts-and-supplies/1")
                .then()
                .body("id", equalTo(1))
                .body("code", equalTo("P001"))
                .body("description", equalTo("Filtro de óleo"))
                .body("price", equalTo(35.90F))
                .body("manufacturer", equalTo("Bosch"))
                .body("quantity", equalTo(10))
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void createPartsAndSupply() {
        JsonObject jsonObject =
                Json.createObjectBuilder()
                        .add("code", "P004")
                        .add("description", "Lâmpada de farol")
                        .add("manufacturer", "Philips")
                        .add("type", "UNITARY")
                        .add("price", "99.99")
                        .add("quantity", "60")
                        .build();

        given()
                .contentType("application/json")
                .body(jsonObject.toString())
                .when()
                .post("/parts-and-supplies")
                .then()
                .body("id", notNullValue())
                .body("code", equalTo("P004"))
                .body("description", equalTo("Lâmpada de farol"))
                .body("price", equalTo(99.99F))
                .body("manufacturer", equalTo("Philips"))
                .body("quantity", equalTo(60))
                .statusCode(Response.Status.CREATED.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void updatePartsAndSupply() {

        JsonObject jsonObject =
                Json.createObjectBuilder()
                        .add("id", 1L)
                        .add("code", "P001")
                        .add("description", "Filtro de óleo")
                        .add("price", "39.99")
                        .add("manufacturer", "Bosch")
                        .add("type", "UNITARY")
                        .add("quantity", "10")
                        .build();

        given()
                .contentType("application/json")
                .body(jsonObject.toString())
                .when()
                .put("/parts-and-supplies/1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        given()
                .when()
                .get("/parts-and-supplies/1")
                .then()
                .body("id", equalTo(1))
                .body("code", equalTo("P001"))
                .body("description", equalTo("Filtro de óleo"))
                .body("price", equalTo(39.99F))
                .body("manufacturer", equalTo("Bosch"))
                .body("quantity", equalTo(10))
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void deletePartsAndSupply() {
        given()
                .when()
                .delete("/parts-and-supplies/1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        given()
                .when()
                .get("/parts-and-supplies/1")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "testUser", roles = "ADMIN")
    void orderPartsAndSupply() {
        given()
                .param("quantity", "1")
                .when()
                .patch("/parts-and-supplies/2/order")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        given()
                .when()
                .get("/parts-and-supplies/2")
                .then()
                .body("id", equalTo(2))
                .body("code", equalTo("P002"))
                .body("description", equalTo("Pastilha de freio"))
                .body("price", equalTo(120.0F))
                .body("manufacturer", equalTo("Valeo"))
                .body("quantity", equalTo(4))
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
