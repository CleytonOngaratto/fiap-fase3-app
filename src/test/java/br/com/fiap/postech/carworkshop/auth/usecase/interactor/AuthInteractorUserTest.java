package br.com.fiap.postech.carworkshop.auth.usecase.interactor;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
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

class AuthInteractorUserTest {

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
    void findByUsername_exists_returnsUser() {
        User user = User.builder().id(1L).username("admin").roles(List.of(UserRoles.ADMIN)).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        Optional<User> result = userRepository.findByUsername("admin");
        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
    }

    @Test
    void findByUsername_notFound_throwsEntityNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> interactor.login(new LoginRequest("nonexistent", "pass")));
    }

    @Test
    void signup_nullPassword_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> interactor.signup(new SignupRequest("admin", null, List.of(UserRoles.ADMIN))));
        verifyNoInteractions(userRepository);
    }

    @Test
    void signup_emptyUsername_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> interactor.signup(new SignupRequest("", "password", List.of(UserRoles.ADMIN))));
        verifyNoInteractions(userRepository);
    }

    @Test
    void save_persistsNewUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(security.generateSalt()).thenReturn("salt");
        when(security.hashPassword(anyString(), anyString(), anyString())).thenReturn("hash");
        User saved = User.builder().id(1L).username("newuser").roles(List.of(UserRoles.USER)).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = interactor.signup(new SignupRequest("newuser", "password", List.of(UserRoles.USER)));
        assertNotNull(result);
        assertEquals("newuser", result.username());
        verify(userRepository).save(any(User.class));
    }
}
