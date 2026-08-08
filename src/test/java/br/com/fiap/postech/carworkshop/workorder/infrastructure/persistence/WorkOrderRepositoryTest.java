package br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WorkOrderRepositoryTest {

    @Inject
    WorkOrderPanacheRepository workOrderRepository;

    // Isolamento: @TestTransaction reverte tudo ao final; o deleteAll() (clean slate — este teste
    // checa findByStatus(COMPLETED).isEmpty()) roda dentro da transação e é desfeito no rollback,
    // preservando o seed compartilhado para as outras classes.

    @Test
    @TestTransaction
    void testFindByStatus_should_return_work_orders_with_received_status() {
        workOrderRepository.deleteAll();
        var workOrder = WorkOrderJpaEntity.builder()
                .customerId(1L).customerEmail("joao@email.com")
                .vehicleId(1L).vehiclePlate("ABC-1234")
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of()).parts(List.of())
                .build();
        workOrderRepository.persist(workOrder);

        var result = workOrderRepository.findByStatus(StatusWO.RECEIVED);

        assertFalse(result.isEmpty());
        assertEquals(StatusWO.RECEIVED, result.get(0).getStatus());
    }

    @Test
    @TestTransaction
    void testFindByStatus_should_return_empty_when_no_orders_with_status() {
        workOrderRepository.deleteAll();
        var result = workOrderRepository.findByStatus(StatusWO.COMPLETED);
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    void testPersistAndFindById_should_work_correctly() {
        workOrderRepository.deleteAll();
        var workOrder = WorkOrderJpaEntity.builder()
                .customerId(2L).customerEmail("maria@email.com")
                .vehicleId(2L).vehiclePlate("DEF-5678")
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of()).parts(List.of())
                .build();
        workOrderRepository.persist(workOrder);

        var result = workOrderRepository.findByIdOptional(workOrder.id);

        assertTrue(result.isPresent());
        assertEquals(StatusWO.RECEIVED, result.get().getStatus());
        assertEquals(2L, result.get().getCustomerId());
        assertEquals("maria@email.com", result.get().getCustomerEmail());
    }
}
