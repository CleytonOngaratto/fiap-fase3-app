package br.com.fiap.postech.carworkshop.auth.infrastructure.config;

import br.com.fiap.postech.carworkshop.auth.infrastructure.config.AuthModuleConfig;
import br.com.fiap.postech.carworkshop.auth.usecase.interactor.AuthInteractor;
import br.com.fiap.postech.carworkshop.auth.usecase.port.in.AuthUseCase;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.SecurityPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.TokenProviderPort;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AuthModuleConfigTest {

    @Test
    void authUseCase_buildsWiredInteractor() {
        AuthUseCase useCase = new AuthModuleConfig().authUseCase(
                mock(UserRepositoryPort.class),
                mock(TokenProviderPort.class),
                mock(SecurityPort.class));

        assertNotNull(useCase);
        assertInstanceOf(AuthInteractor.class, useCase);
    }
}
