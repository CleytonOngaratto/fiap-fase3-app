package br.com.fiap.postech.carworkshop.autoservice.infrastructure.config;

import br.com.fiap.postech.carworkshop.autoservice.infrastructure.config.AutoServiceModuleConfig;
import br.com.fiap.postech.carworkshop.autoservice.usecase.interactor.AutoServiceInteractor;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.in.AutoServiceUseCase;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AutoServiceModuleConfigTest {

    @Test
    void autoServiceUseCase_buildsWiredInteractor() {
        AutoServiceUseCase useCase = new AutoServiceModuleConfig().autoServiceUseCase(
                mock(AutoServiceRepositoryPort.class));

        assertNotNull(useCase);
        assertInstanceOf(AutoServiceInteractor.class, useCase);
    }
}
