package br.com.fiap.postech.carworkshop.auth.usecase.interactor;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.AuthTokenResponse;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.UserResponse;
import br.com.fiap.postech.carworkshop.auth.domain.entity.User;
import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;
import br.com.fiap.postech.carworkshop.auth.usecase.interactor.AuthInteractor;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.SecurityPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.TokenProviderPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.UserRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthInteractorTest {

    @InjectMocks
    AuthInteractor interactor;

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    TokenProviderPort tokenProvider;

    @Mock
    SecurityPort security;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_shouldCreateUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(security.generateSalt()).thenReturn("salt");
        when(security.hashPassword(anyString(), anyString(), anyString())).thenReturn("hashedPwd");
        User saved = User.builder().id(1L).username("admin").roles(List.of(UserRoles.ADMIN)).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        SignupRequest request = new SignupRequest("admin", "password", List.of(UserRoles.ADMIN));
        UserResponse result = interactor.signup(request);

        assertNotNull(result);
        assertEquals("admin", result.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_existingUsername_throwsValidation() {
        User existing = User.builder().id(1L).username("admin").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        SignupRequest request = new SignupRequest("admin", "password", List.of(UserRoles.ADMIN));
        assertThrows(ValidationException.class, () -> interactor.signup(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_nullUsername_throwsValidation() {
        SignupRequest request = new SignupRequest(null, "password", List.of(UserRoles.ADMIN));
        assertThrows(ValidationException.class, () -> interactor.signup(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void signup_emptyRoles_throwsValidation() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        SignupRequest request = new SignupRequest("admin", "password", List.of());
        assertThrows(ValidationException.class, () -> interactor.signup(request));
    }

    @Test
    void login_shouldReturnToken() {
        User user = User.builder().username("admin").password("hashedPwd").salt("salt")
                .roles(List.of(UserRoles.ADMIN)).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(security.hashPassword("admin", "password", "salt")).thenReturn("hashedPwd");
        when(tokenProvider.generateToken(anyString(), any())).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest("admin", "password");
        AuthTokenResponse result = interactor.login(request);

        assertNotNull(result);
        assertEquals("jwt-token", result.token());
    }

    @Test
    void login_userNotFound_throwsEntityNotFound() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.login(new LoginRequest("admin", "pass")));
    }

    @Test
    void login_wrongPassword_throwsValidation() {
        User user = User.builder().username("admin").password("correctHash").salt("salt")
                .roles(List.of(UserRoles.ADMIN)).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(security.hashPassword("admin", "wrongpass", "salt")).thenReturn("wrongHash");

        assertThrows(ValidationException.class, () -> interactor.login(new LoginRequest("admin", "wrongpass")));
    }
}
