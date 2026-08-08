package br.com.fiap.postech.carworkshop.customer.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;
import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MapperServicePlainTest {

    @Test
    void autoServiceResponse_shouldMapFromDomain() {
        AutoService domain = AutoService.builder().id(1L).description("Oil Change").price(new BigDecimal("99.99")).build();
        AutoServiceResponse response = AutoServiceResponse.from(domain);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Oil Change", response.description());
        assertEquals(new BigDecimal("99.99"), response.price());
    }

    @Test
    void customerResponse_shouldMapFromDomain() {
        Customer domain = Customer.builder()
                .id(1L).name("João").document("12345678909").rg("MG123")
                .email("joao@test.com").number("11999999999")
                .build();
        CustomerResponse response = CustomerResponse.from(domain);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("João", response.name());
        assertEquals("12345678909", response.document());
        assertEquals("joao@test.com", response.email());
    }

    @Test
    void autoServiceResponse_shouldHandleNullValues() {
        AutoService domain = AutoService.builder().id(1L).description(null).price(null).build();
        AutoServiceResponse response = AutoServiceResponse.from(domain);
        assertNull(response.description());
        assertNull(response.price());
    }
}
