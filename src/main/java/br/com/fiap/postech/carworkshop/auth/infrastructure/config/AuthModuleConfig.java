package br.com.fiap.postech.carworkshop.auth.infrastructure.config;

import br.com.fiap.postech.carworkshop.auth.usecase.interactor.AuthInteractor;
import br.com.fiap.postech.carworkshop.auth.usecase.port.in.AuthUseCase;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.SecurityPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.TokenProviderPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the auth use case. The framework annotation lives HERE so the
 * {@link AuthInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Reference pattern for the other modules.
 */
@ApplicationScoped
public class AuthModuleConfig {

    @Produces
    @ApplicationScoped
    public AuthUseCase authUseCase(UserRepositoryPort userRepository,
                                   TokenProviderPort tokenProvider,
                                   SecurityPort security) {
        return new AuthInteractor(userRepository, tokenProvider, security);
    }
}
