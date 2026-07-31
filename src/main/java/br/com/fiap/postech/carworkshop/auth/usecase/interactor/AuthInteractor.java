package br.com.fiap.postech.carworkshop.auth.usecase.interactor;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.AuthTokenResponse;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.UserResponse;
import br.com.fiap.postech.carworkshop.auth.domain.entity.User;
import br.com.fiap.postech.carworkshop.auth.usecase.port.in.AuthUseCase;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.SecurityPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.TokenProviderPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.UserRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;

public class AuthInteractor implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenProviderPort tokenProvider;
    private final SecurityPort security;

    public AuthInteractor(UserRepositoryPort userRepository,
                          TokenProviderPort tokenProvider,
                          SecurityPort security) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.security = security;
    }

    @Override
    public UserResponse signup(SignupRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new ValidationException("Username must not be blank.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ValidationException("Password must not be blank.");
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new ValidationException("At least one role is required.");
        }
        userRepository.findByUsername(request.username())
                .ifPresent(u -> { throw new ValidationException("Username already exists."); });

        String salt = security.generateSalt();
        String hashedPassword = security.hashPassword(request.username(), request.password(), salt);

        User user = User.builder()
                .username(request.username())
                .password(hashedPassword)
                .salt(salt)
                .roles(request.roles())
                .build();

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        String hashedPassword = security.hashPassword(request.username(), request.password(), user.getSalt());
        if (!hashedPassword.equals(user.getPassword())) {
            throw new ValidationException("Invalid username or password.");
        }

        String token = tokenProvider.generateToken(request.username(), user.getRoles());
        return new AuthTokenResponse(token);
    }
}
