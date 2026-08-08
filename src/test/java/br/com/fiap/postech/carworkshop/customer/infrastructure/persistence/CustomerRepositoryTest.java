package br.com.fiap.postech.carworkshop.customer.infrastructure.persistence;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CustomerRepositoryTest {

    @Inject
    CustomerPanacheRepository customerRepository;

    // Isolamento: @TestTransaction reverte tudo ao final de cada teste, então o deleteAll() (clean
    // slate) roda DENTRO da transação e é desfeito no rollback — o seed compartilhado do H2 fica
    // intacto para as outras classes @QuarkusTest (independe da ordem de execução).

    @Test
    @TestTransaction
    void testFindByDocument_should_return_customer_when_document_exists() {
        customerRepository.deleteAll();
        var customer = CustomerJpaEntity.builder()
                .name("João Silva").document("12345678900")
                .rg("MG1234567").email("joao@email.com").number("11999999999")
                .build();
        customerRepository.persist(customer);

        var result = customerRepository.findByDocument("12345678900");

        assertTrue(result.isPresent());
        assertEquals("João Silva", result.get().getName());
        assertEquals("12345678900", result.get().getDocument());
    }

    @Test
    @TestTransaction
    void testFindByDocument_should_return_empty_when_document_does_not_exist() {
        customerRepository.deleteAll();
        var result = customerRepository.findByDocument("99999999999");
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByEmail_should_return_customer_when_email_exists() {
        customerRepository.deleteAll();
        var customer = CustomerJpaEntity.builder()
                .name("Maria Santos").document("98765432100")
                .rg("SP9876543").email("maria@email.com").number("11988888888")
                .build();
        customerRepository.persist(customer);

        var result = customerRepository.findByEmail("maria@email.com");

        assertTrue(result.isPresent());
        assertEquals("Maria Santos", result.get().getName());
    }

    @Test
    @TestTransaction
    void testFindByEmail_should_return_empty_when_email_does_not_exist() {
        customerRepository.deleteAll();
        var result = customerRepository.findByEmail("notfound@email.com");
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    void testPersistAndFindById_should_work_correctly() {
        customerRepository.deleteAll();
        var customer = CustomerJpaEntity.builder()
                .name("Pedro Costa").document("11122233344")
                .rg("RJ1112223").email("pedro@email.com").number("21977777777")
                .build();

        customerRepository.persist(customer);
        var result = customerRepository.findByIdOptional(customer.id);

        assertTrue(result.isPresent());
        assertEquals("Pedro Costa", result.get().getName());
    }
}
