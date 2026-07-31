package br.com.fiap.postech.carworkshop.auth.adapter.controller;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.AuthTokenResponse;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.UserResponse;
import br.com.fiap.postech.carworkshop.auth.usecase.port.in.AuthUseCase;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class AuthControllerTest {

    @InjectMock
    AuthUseCase useCase;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private AuthTokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest("test@test.com", "password123", List.of());
        loginRequest = new LoginRequest("test@test.com", "password123");
        userResponse = new UserResponse(1L, "test@test.com", List.of());
        tokenResponse = new AuthTokenResponse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    }

    @Test
    void testCreateAccount_Success() {
        Mockito.when(useCase.signup(any(SignupRequest.class))).thenReturn(userResponse);

        given().contentType(MediaType.APPLICATION_JSON).body(signupRequest)
                .when().post("/auth/signup").then()
                .statusCode(201).body("id", is(1)).body("username", is("test@test.com"));

        Mockito.verify(useCase).signup(any(SignupRequest.class));
    }

    @Test
    void testLogin_Success() {
        Mockito.when(useCase.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        given().contentType(MediaType.APPLICATION_JSON).body(loginRequest)
                .when().post("/auth/login").then()
                .statusCode(200).body("token", is("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."));

        Mockito.verify(useCase).login(any(LoginRequest.class));
    }

    @Test
    void testCreateAccount_InvalidData() {
        Mockito.when(useCase.signup(any(SignupRequest.class)))
                .thenThrow(new ValidationException("Username must not be blank."));

        given().contentType(MediaType.APPLICATION_JSON).body(signupRequest)
                .when().post("/auth/signup").then().statusCode(400);
    }

    @Test
    void testLogin_InvalidCredentials() {
        Mockito.when(useCase.login(any(LoginRequest.class)))
                .thenThrow(new ValidationException("Invalid username or password."));

        given().contentType(MediaType.APPLICATION_JSON).body(loginRequest)
                .when().post("/auth/login").then().statusCode(400);
    }
}
