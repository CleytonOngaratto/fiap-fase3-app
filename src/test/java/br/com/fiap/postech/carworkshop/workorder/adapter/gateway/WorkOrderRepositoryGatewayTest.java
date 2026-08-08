package br.com.fiap.postech.carworkshop.workorder.adapter.gateway;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderPanacheRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WorkOrderRepositoryGatewayTest {

    @Inject
    WorkOrderRepositoryGateway gateway;

    @Inject
    WorkOrderPanacheRepository workOrderRepository;

    // Isolamento: @TestTransaction reverte tudo ao final; o deleteAll() (clean slate) roda dentro da
    // transação e é desfeito no rollback, preservando o seed compartilhado para as outras classes.

    @Test
    @TestTransaction
    void save_then_findById_preserves_customer_vehicle_and_snapshots() {
        workOrderRepository.deleteAll();
        AutoService service = AutoService.builder()
                .id(10L).description("Troca de Óleo").price(new BigDecimal("150.00")).build();
        PartsAndSupply part = PartsAndSupply.builder()
                .id(20L).code("P001").manufacturer("Bosch").description("Filtro de óleo")
                .price(new BigDecimal("35.90")).type(TypeProductEnum.UNITARY).quantity(10).build();

        WorkOrder toSave = WorkOrder.builder()
                .customerId(1L).customerEmail("john.silva@email.com")
                .vehicleId(3L).vehiclePlate("DEF9G12")
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of(service)).parts(List.of(part))
                .build();

        WorkOrder saved = gateway.save(toSave);
        assertNotNull(saved.getId());

        Optional<WorkOrder> reloadedOpt = gateway.findById(saved.getId());
        assertTrue(reloadedOpt.isPresent());
        WorkOrder reloaded = reloadedOpt.get();

        assertEquals(1L, reloaded.getCustomerId());
        assertEquals("john.silva@email.com", reloaded.getCustomerEmail());
        assertEquals(3L, reloaded.getVehicleId());
        assertEquals("DEF9G12", reloaded.getVehiclePlate());

        assertEquals(1, reloaded.getServices().size());
        AutoService reloadedService = reloaded.getServices().get(0);
        assertEquals(10L, reloadedService.getId());
        assertEquals("Troca de Óleo", reloadedService.getDescription());
        assertEquals(new BigDecimal("150.00"), reloadedService.getPrice());

        assertEquals(1, reloaded.getParts().size());
        PartsAndSupply reloadedPart = reloaded.getParts().get(0);
        assertEquals(20L, reloadedPart.getId());
        assertEquals("P001", reloadedPart.getCode());
        assertEquals("Bosch", reloadedPart.getManufacturer());
        assertEquals("Filtro de óleo", reloadedPart.getDescription());
        assertEquals(new BigDecimal("35.90"), reloadedPart.getPrice());
        assertEquals(TypeProductEnum.UNITARY, reloadedPart.getType());
        assertEquals(10, reloadedPart.getQuantity());
    }
}
