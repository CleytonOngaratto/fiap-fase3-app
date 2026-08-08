package br.com.fiap.postech.carworkshop.customer.infrastructure.config;

import br.com.fiap.postech.carworkshop.customer.infrastructure.config.CustomerModuleConfig;
import br.com.fiap.postech.carworkshop.customer.usecase.interactor.CustomerInteractor;
import br.com.fiap.postech.carworkshop.customer.usecase.port.in.CustomerUseCase;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class CustomerModuleConfigTest {

    @Test
    void customerUseCase_buildsWiredInteractor() {
        CustomerUseCase useCase = new CustomerModuleConfig().customerUseCase(
                mock(CustomerRepositoryPort.class));

        assertNotNull(useCase);
        assertInstanceOf(CustomerInteractor.class, useCase);
    }
}
