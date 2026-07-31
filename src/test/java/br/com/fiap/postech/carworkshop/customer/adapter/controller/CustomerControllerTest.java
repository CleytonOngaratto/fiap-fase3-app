package br.com.fiap.postech.carworkshop.customer.adapter.controller;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;
import br.com.fiap.postech.carworkshop.customer.usecase.port.in.CustomerUseCase;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class CustomerControllerTest {

    @InjectMock
    CustomerUseCase useCase;

    private CustomerResponse customerResponse;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        customerResponse = new CustomerResponse(1L, "João Silva", "12345678900", "MG1234567",
                "joao@email.com", "11999999999");
        customerRequest = new CustomerRequest("João Silva", "12345678900", "MG1234567",
                "joao@email.com", "11999999999");
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindAllCustomers_Success() {
        CustomerResponse response2 = new CustomerResponse(2L, "Maria Santos", "98765432100",
                "SP9876543", "maria@email.com", "11988888888");
        Mockito.when(useCase.findAll()).thenReturn(Arrays.asList(customerResponse, response2));

        given().when().get("/customers/get-all").then()
                .statusCode(200).body("size()", is(2))
                .body("[0].name", is("João Silva")).body("[1].name", is("Maria Santos"));

        Mockito.verify(useCase).findAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerById_Success() {
        Mockito.when(useCase.findById(1L)).thenReturn(customerResponse);

        given().when().get("/customers/get-by-id/1").then()
                .statusCode(200).body("id", is(1)).body("name", is("João Silva"))
                .body("document", is("12345678900"));

        Mockito.verify(useCase).findById(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerById_NotFound() {
        Mockito.when(useCase.findById(999L)).thenThrow(new EntityNotFoundException("Customer not found."));
        given().when().get("/customers/get-by-id/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerByDocument_Success() {
        Mockito.when(useCase.findByDocument("12345678900")).thenReturn(customerResponse);

        given().when().get("/customers/get-by-document/12345678900").then()
                .statusCode(200).body("document", is("12345678900")).body("name", is("João Silva"));

        Mockito.verify(useCase).findByDocument("12345678900");
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerByDocument_NotFound() {
        Mockito.when(useCase.findByDocument("00000000000")).thenThrow(new EntityNotFoundException("Customer not found."));
        given().when().get("/customers/get-by-document/00000000000").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerByEmail_Success() {
        Mockito.when(useCase.findByEmail("joao@email.com")).thenReturn(customerResponse);

        given().when().get("/customers/get-by-email/joao@email.com").then()
                .statusCode(200).body("email", is("joao@email.com")).body("name", is("João Silva"));

        Mockito.verify(useCase).findByEmail("joao@email.com");
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testFindCustomerByEmail_NotFound() {
        Mockito.when(useCase.findByEmail("notfound@email.com"))
                .thenThrow(new EntityNotFoundException("Customer not found."));
        given().when().get("/customers/get-by-email/notfound@email.com").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateCustomer_Success() {
        Mockito.when(useCase.create(any(CustomerRequest.class))).thenReturn(customerResponse);

        given().contentType(MediaType.APPLICATION_JSON).body(customerRequest)
                .when().post("/customers/create").then()
                .statusCode(201).body("name", is("João Silva")).body("document", is("12345678900"));

        Mockito.verify(useCase).create(any(CustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testCreateCustomer_BadRequest() {
        Mockito.when(useCase.create(any(CustomerRequest.class)))
                .thenThrow(new ValidationException("Customer with this CPF/CNPJ already exists."));

        given().contentType(MediaType.APPLICATION_JSON).body(customerRequest)
                .when().post("/customers/create").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testUpdateCustomer_Success() {
        Mockito.doNothing().when(useCase).update(eq(1L), any(CustomerRequest.class));

        given().contentType(MediaType.APPLICATION_JSON).body(customerRequest)
                .when().put("/customers/update/1").then().statusCode(204);

        Mockito.verify(useCase).update(eq(1L), any(CustomerRequest.class));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testUpdateCustomer_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Customer not found."))
                .when(useCase).update(eq(999L), any(CustomerRequest.class));

        given().contentType(MediaType.APPLICATION_JSON).body(customerRequest)
                .when().put("/customers/update/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeleteCustomer_Success() {
        Mockito.doNothing().when(useCase).delete(1L);
        given().when().delete("/customers/delete/1").then().statusCode(204);
        Mockito.verify(useCase).delete(1L);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testDeleteCustomer_NotFound() {
        Mockito.doThrow(new EntityNotFoundException("Customer not found.")).when(useCase).delete(999L);
        given().when().delete("/customers/delete/999").then().statusCode(404);
    }

    @Test
    void testFindAllCustomers_Unauthorized() {
        given().when().get("/customers/get-all").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "user", roles = "USER")
    void testCreateCustomer_Forbidden() {
        given().contentType(MediaType.APPLICATION_JSON).body(customerRequest)
                .when().post("/customers/create").then().statusCode(403);
    }
}
