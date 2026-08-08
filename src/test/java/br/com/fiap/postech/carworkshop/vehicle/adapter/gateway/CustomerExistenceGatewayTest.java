package br.com.fiap.postech.carworkshop.vehicle.adapter.gateway;

import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import br.com.fiap.postech.carworkshop.vehicle.adapter.gateway.CustomerExistenceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V3: the gateway delegates customer existence to the customer module's public port,
 * not its Panache repository. Trivial unit test guards the delegation (and coverage).
 */
class CustomerExistenceGatewayTest {

    @InjectMocks
    CustomerExistenceGateway gateway;
    @Mock
    CustomerRepositoryPort customerRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void existsById_returnsTrue_whenCustomerFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mock(Customer.class)));
        assertTrue(gateway.existsById(1L));
    }

    @Test
    void existsById_returnsFalse_whenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(gateway.existsById(99L));
    }
}
